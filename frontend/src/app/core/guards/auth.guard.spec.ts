import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard, supportRoleGuard } from './auth.guard';
import { AuthService } from '../auth/auth.service';

describe('authGuard', () => {
  it('redirects unauthenticated users to login', () => {
    const tree={redirect:true};const router={createUrlTree:vi.fn().mockReturnValue(tree)};
    TestBed.configureTestingModule({providers:[{provide:AuthService,useValue:{isAuthenticated:()=>false}},{provide:Router,useValue:router}]});
    const result=TestBed.runInInjectionContext(()=>authGuard({} as never,{} as never));
    expect(result).toBe(tree);expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);
  });
});

describe('supportRoleGuard',()=>{
  it('redirects USER to tickets',()=>{const tree={redirect:true};const router={createUrlTree:vi.fn().mockReturnValue(tree)};TestBed.configureTestingModule({providers:[{provide:AuthService,useValue:{getCurrentUser:()=>({role:'USER'})}},{provide:Router,useValue:router}]});expect(TestBed.runInInjectionContext(()=>supportRoleGuard({}as never,{}as never))).toBe(tree);expect(router.createUrlTree).toHaveBeenCalledWith(['/app/tickets']);});
  it.each(['SUPPORT_N1','SUPPORT_N2','ADMIN'])('allows %s',role=>{TestBed.resetTestingModule();TestBed.configureTestingModule({providers:[{provide:AuthService,useValue:{getCurrentUser:()=>({role})}},{provide:Router,useValue:{createUrlTree:vi.fn()}}]});expect(TestBed.runInInjectionContext(()=>supportRoleGuard({}as never,{}as never))).toBe(true);});
});
