export enum TicketStatus {
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  CODE_REVIEW = 'CODE_REVIEW',
  TESTING = 'TESTING',
  DONE = 'DONE',
  ESCALATED = 'ESCALATED',
  HUMAN_TODO = 'HUMAN_TODO',
  HUMAN_DEV = 'HUMAN_DEV',
  HUMAN_REVIEW = 'HUMAN_REVIEW',
  HUMAN_TESTING = 'HUMAN_TESTING'
}

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum AgentType {
  PO = 'PO',
  DEVELOPER = 'DEVELOPER',
  REVIEWER = 'REVIEWER',
  TESTER = 'TESTER',
  HUMAN = 'HUMAN'
}

export interface Project {
  id: number;
  name: string;
  description: string;
  createdAt: string;
}

export interface ProjectRequest {
  name: string;
  description: string;
}

export interface Ticket {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: Priority;
  assignedAgent: AgentType | null;
  agentLogs: string[];
  branchName: string | null;
  enableCodeReview: boolean;
  enableTesting: boolean;
  projectId: number | null;
  projectName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketRequest {
  title: string;
  description: string;
  priority?: Priority;
  status?: TicketStatus;
}

export interface TicketStats {
  total: number;
  todo: number;
  inProgress: number;
  codeReview: number;
  testing: number;
  done: number;
  escalated: number;
  humanTodo: number;
  humanDev: number;
  humanReview: number;
  humanTesting: number;
}

export interface RequirementsRequest {
  requirements: string;
  projectId: number | null;
}

export interface AgentLog {
  id: number;
  ticketId: number;
  agentType: AgentType;
  action: string;
  message: string;
  timestamp: string;
}

export interface SseEvent {
  type: string;
  data: {
    ticketId: number;
    status: TicketStatus;
    agent: AgentType;
    message: string;
  };
}
