import { AuthenticatedUser } from './auth.models';

export interface CommentResponse {
  id: string;
  content: string;
  internal: boolean;
  author: AuthenticatedUser;
  createdAt: string;
  updatedAt: string;
}

export interface AddCommentRequest {
  content: string;
  internal: boolean;
}
