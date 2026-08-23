import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AddCommentRequest, CommentResponse } from '../models/comment.models';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly http = inject(HttpClient);

  getComments(ticketId: string) {
    return this.http.get<CommentResponse[]>(`${environment.apiBaseUrl}/api/tickets/${ticketId}/comments`);
  }

  addComment(ticketId: string, request: AddCommentRequest) {
    return this.http.post<CommentResponse>(`${environment.apiBaseUrl}/api/tickets/${ticketId}/comments`, request);
  }
}
