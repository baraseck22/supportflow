import { Routes } from '@angular/router';
import { authGuard, guestGuard, rootGuard, supportRoleGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { AppLayoutComponent } from './layout/app-layout.component';

export const routes: Routes = [
  {path:'',pathMatch:'full',canActivate:[rootGuard],component:LoginComponent},
  {path:'login',canActivate:[guestGuard],component:LoginComponent},
  {path:'app',canActivate:[authGuard],component:AppLayoutComponent,children:[
    {path:'',pathMatch:'full',redirectTo:'tickets'},
    {path:'dashboard',canActivate:[supportRoleGuard],loadComponent:()=>import('./features/dashboard/dashboard.component').then(m=>m.DashboardComponent)},
    {path:'tickets',loadComponent:()=>import('./features/tickets/tickets.component').then(m=>m.TicketsComponent)},
    {path:'tickets/new',loadComponent:()=>import('./features/tickets/ticket-create/ticket-create.component').then(m=>m.TicketCreateComponent)},
    {path:'tickets/:id',loadComponent:()=>import('./features/tickets/ticket-detail.component').then(m=>m.TicketDetailComponent)},
  ]},
  {path:'**',redirectTo:''},
];
