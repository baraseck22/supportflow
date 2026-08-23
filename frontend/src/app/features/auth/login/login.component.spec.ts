import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/auth/auth.service';

describe('LoginComponent', () => {
  const auth={login:vi.fn()};const router={navigateByUrl:vi.fn()};
  beforeEach(async()=>{auth.login.mockReset();router.navigateByUrl.mockReset();await TestBed.configureTestingModule({imports:[LoginComponent],providers:[{provide:AuthService,useValue:auth},{provide:Router,useValue:router}]}).compileComponents();});
  function component(){const fixture=TestBed.createComponent(LoginComponent);fixture.componentInstance.form.setValue({email:'alice.user@supportflow.local',password:'secret'});return fixture.componentInstance;}
  it('shows a clear error on 401',()=>{auth.login.mockReturnValue(throwError(()=>({status:401})));const instance=component();instance.submit();expect(instance.error()).toBe('Email ou mot de passe incorrect');});
  it('redirects to app after successful login',()=>{auth.login.mockReturnValue(of({}));component().submit();expect(router.navigateByUrl).toHaveBeenCalledWith('/app');});
});
