import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
@Component({ selector: 'app-layout', imports:[RouterOutlet,RouterLink,RouterLinkActive], templateUrl: './app-layout.component.html', styleUrl: './app-layout.component.scss' })
export class AppLayoutComponent { private readonly auth=inject(AuthService);private readonly router=inject(Router);readonly user=this.auth.getCurrentUser();readonly isSupport=this.user?.role!=='USER';logout():void{this.auth.logout();void this.router.navigateByUrl('/login');} }
