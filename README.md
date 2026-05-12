# TicketCodeIA

TicketCodeIA is an AI-powered ticket management and development automation platform. It uses Claude AI agents to autonomously develop software based on tickets, while humans supervise and can intervene at any stage.

## Architecture

The project is a full-stack application split into two modules:

- **TicketCodeIABack** - Spring Boot backend (Java 21, Hexagonal Architecture)
- **TicketCodeIA_Frontend** - Angular 21 frontend with Angular Material

```
Backend (Hexagonal / Clean Architecture)
├── domain/          # Entities, business logic, ports
├── application/     # Use cases (commands & queries)
├── infrastructure/  # Adapters (agents, persistence, config)
└── presentation/    # REST controllers, DTOs

Frontend (Angular 21)
├── pages/           # Dashboard, Board, Requirements, Escalations, Human Board, Config
├── components/      # Navbar, StatusBadge, AgentAvatar, TicketDialog
├── services/        # HTTP clients, SSE connection
└── models/          # TypeScript interfaces and enums
```

## How It Works

### Agent Pipeline

The system orchestrates multiple specialized AI agents in a pipeline:

```
EXPERT AGENT (Planning)
│  Chat with user to understand the project
│  Propose features and create tickets
│  Start project processing when user confirms
│
└─► PROJECT PROCESSOR
    │  Selects the best TODO ticket via Expert Agent
    │
    └─► DEVELOPER AGENT
        │  Spawns Claude Code CLI to implement the ticket
        │
        └─► REVIEWER AGENT (optional)
            │  Reviews the generated code for quality and security
            │
            └─► TESTER AGENT (optional)
                │  Runs tests via Claude Code CLI
                │
                └─► DONE (or escalate to human after max retries)
```

### Ticket Lifecycle

```
TODO ──► IN_PROGRESS ──► CODE_REVIEW ──► TESTING ──► DONE
              │                │              │
              ▼                ▼              ▼
         HUMAN_DEV ──► HUMAN_REVIEW ──► HUMAN_TESTING ──► DONE
```

When an agent fails after the configured max retries, the ticket is escalated to the human board for manual intervention.

### AI Models Used

| Component | Model | Method |
|-----------|-------|--------|
| Expert Agent | claude-sonnet-4-5 | Spring AI ChatClient |
| Reviewer Agent | claude-sonnet-4-5 | Spring AI ChatClient |
| Developer Agent | Configurable | Claude Code CLI |
| Tester Agent | Configurable | Claude Code CLI |

### Key Workflow

1. **Create a project** on the Requirements page
2. **Chat with the Expert Agent** - describe your app idea, the agent asks clarifying questions and proposes tickets
3. **Confirm ticket creation** - the expert agent creates tickets via tool calls
4. **Start the project** - the agent pipeline begins processing tickets autonomously
5. **Monitor progress** on the Kanban Board (real-time updates via SSE)
6. **Handle escalations** - intervene manually on the Human Board when agents get stuck

## Prerequisites

- Java 21+
- Maven
- Node.js 20+
- npm
- Claude Code CLI installed
- An Anthropic API key

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `ANTHROPIC_API_KEY` | Your Anthropic API key | *required* |
| `SPRING_PROFILES_ACTIVE` | Spring profile (`dev` or `prod`) | `prod` |
| `TICKCODE_WORKSPACE` | Directory where agents write code | `C:/tickcode-workspace` |

## Running the Project

### Backend

```bash
cd TicketCodeIABack
export ANTHROPIC_API_KEY=sk-ant-...
mvn clean install
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

### Frontend

```bash
cd TicketCodeIA_Frontend
npm install
ng serve
```

The frontend starts on `http://localhost:4200`.

## Database

- **Development**: H2 in-memory database (no setup needed, schema auto-created)
- **Production**: PostgreSQL (configure in `application-prod.properties`)

## REST API

### Tickets
- `GET /api/tickets` - List tickets (filters: `projectId`, `status`)
- `POST /api/tickets` - Create a ticket
- `GET /api/tickets/{id}` - Get ticket details
- `PUT /api/tickets/{id}` - Update a ticket
- `DELETE /api/tickets/{id}` - Delete a ticket
- `POST /api/tickets/{id}/process` - Start the agent pipeline on a ticket
- `GET /api/tickets/{id}/logs` - Get agent activity logs
- `GET /api/tickets/stats` - Get ticket statistics
- `POST /api/tickets/{id}/move-to-human-board` - Escalate to human board
- `PUT /api/tickets/{id}/human-board-status?status=X` - Update human board status

### Projects
- `GET /api/projects` - List projects
- `POST /api/projects` - Create a project
- `GET /api/projects/{id}` - Get project details

### Expert Agent
- `POST /api/agents/expert/chat` - Send a message to the expert agent
- `GET /api/agents/expert/project/{projectId}/history` - Get chat history
- `DELETE /api/agents/expert/session/{sessionId}` - Clear a session

### Real-Time Events
- `GET /api/sse/subscribe` - SSE event stream for live updates
- `GET /api/sse/recent-activity` - Recent agent activity

### Configuration
- `GET /api/config/agents` - Get agent configuration
- `PUT /api/config/agents` - Update agent configuration at runtime

## Tech Stack

**Backend**: Spring Boot 3.3.6, Spring AI 1.0.5 (Anthropic), JPA/Hibernate, H2/PostgreSQL, Lombok

**Frontend**: Angular 21, Angular Material, RxJS, TypeScript 5.9

**AI**: Claude (Anthropic API via Spring AI), Claude Code CLI
