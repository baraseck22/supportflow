import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { Ticket } from '../../../core/models/ticket.models';
import { CategoryService } from '../../../core/services/category.service';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketCreateComponent } from './ticket-create.component';

describe('TicketCreateComponent',()=>{
 const categories=[{id:'category-1',name:'ACCESS',description:'Accès'},{id:'category-2',name:'DATA',description:null}];
 const created={id:'ticket-42'} as Ticket;const categoryService={getCategories:vi.fn()};const ticketService={createTicket:vi.fn()};const router={navigate:vi.fn()};
 async function render(result=of(categories)){categoryService.getCategories.mockReturnValue(result);await TestBed.configureTestingModule({imports:[TicketCreateComponent],providers:[{provide:CategoryService,useValue:categoryService},{provide:TicketService,useValue:ticketService},{provide:Router,useValue:router}]}).compileComponents();const fixture=TestBed.createComponent(TicketCreateComponent);fixture.detectChanges();return fixture;}
 beforeEach(()=>{TestBed.resetTestingModule();categoryService.getCategories.mockReset();ticketService.createTicket.mockReset();router.navigate.mockReset()});
 it('loads and displays categories',async()=>{const f=await render();expect(categoryService.getCategories).toHaveBeenCalled();const text=f.nativeElement.querySelector('#category').textContent;expect(text).toContain('ACCESS');expect(text).toContain('DATA');});
 it('requires a non-blank title',async()=>{const f=await render();f.componentInstance.form.controls.title.setValue('   ');expect(f.componentInstance.form.controls.title.invalid).toBe(true);});
 it('requires a non-blank description',async()=>{const f=await render();f.componentInstance.form.controls.description.setValue('   ');expect(f.componentInstance.form.controls.description.invalid).toBe(true);});
 it('requires priority and category',async()=>{const f=await render();expect(f.componentInstance.form.controls.priority.invalid).toBe(true);expect(f.componentInstance.form.controls.categoryId.invalid).toBe(true);});
 it('posts the exact body without creator identity',async()=>{ticketService.createTicket.mockReturnValue(new Subject<Ticket>());const f=await render();f.componentInstance.form.setValue({title:' Incident ',description:' Description ',priority:'HIGH',categoryId:'category-1'});f.componentInstance.submit();const expected={title:'Incident',description:'Description',priority:'HIGH',categoryId:'category-1'};expect(ticketService.createTicket).toHaveBeenCalledWith(expected);expect((ticketService.createTicket.mock.calls[0][0]as Record<string,unknown>)['createdByUserId']).toBeUndefined();});
 it('redirects to the created ticket',async()=>{ticketService.createTicket.mockReturnValue(of(created));const f=await render();f.componentInstance.form.setValue({title:'Incident',description:'Description',priority:'HIGH',categoryId:'category-1'});f.componentInstance.submit();expect(router.navigate).toHaveBeenCalledWith(['/app/tickets','ticket-42']);});
 it.each([[400,'Les informations saisies sont invalides.'],[403,"Vous n'êtes pas autorisé à créer un ticket."],[404,'La catégorie sélectionnée est introuvable.']] as [number,string][])('handles HTTP %i',async(status,message)=>{ticketService.createTicket.mockReturnValue(throwError(()=>new HttpErrorResponse({status})));const f=await render();f.componentInstance.form.setValue({title:'Incident',description:'Description',priority:'HIGH',categoryId:'category-1'});f.componentInstance.submit();f.detectChanges();expect(f.nativeElement.textContent).toContain(message);});
 it('prevents double submission',async()=>{ticketService.createTicket.mockReturnValue(new Subject<Ticket>());const f=await render();f.componentInstance.form.setValue({title:'Incident',description:'Description',priority:'HIGH',categoryId:'category-1'});f.componentInstance.submit();f.componentInstance.submit();expect(ticketService.createTicket).toHaveBeenCalledTimes(1);});
});
