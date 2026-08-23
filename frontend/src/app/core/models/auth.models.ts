export type Role = 'USER' | 'SUPPORT_N1' | 'SUPPORT_N2' | 'ADMIN';
export interface AuthenticatedUser { id: string; firstName: string; lastName: string; email: string; role: Role; }
export interface LoginRequest { email: string; password: string; }
export interface LoginResponse { accessToken: string; tokenType: 'Bearer'; expiresIn: number; user: AuthenticatedUser; }
