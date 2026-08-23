import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CategorySummary, CreateTicketRequest, TicketPriority } from '../../../core/models/ticket.models';
import { CategoryService } from '../../../core/services/category.service';
import { TicketService } from '../../../core/services/ticket.service';

@Component({selector:'app-ticket-create',imports:[ReactiveFormsModule],templateUrl:'./ticket-create.component.html',styleUrl:'./ticket-create.component.scss'})
export class TicketCreateComponent implements OnInit{
 private readonly categoryService=inject(CategoryService);private readonly ticketService=inject(TicketService);private readonly router=inject(Router);
 readonly categories=signal<CategorySummary[]>([]);readonly categoriesLoading=signal(true);readonly categoriesError=signal('');readonly creating=signal(false);readonly createError=signal('');
 readonly priorities:{value:TicketPriority;label:string}[]=[{value:'LOW',label:'Faible'},{value:'MEDIUM',label:'Moyenne'},{value:'HIGH',label:'Haute'},{value:'CRITICAL',label:'Critique'}];
 readonly form=new FormGroup({title:new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.maxLength(200),c=>c.value.trim()?null:{blank:true}]}),description:new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.maxLength(10000),c=>c.value.trim()?null:{blank:true}]}),priority:new FormControl<TicketPriority|null>(null,Validators.required),categoryId:new FormControl('',{nonNullable:true,validators:[Validators.required]})});
 ngOnInit(){this.loadCategories()}
 loadCategories(){this.categoriesLoading.set(true);this.categoriesError.set('');this.categoryService.getCategories().subscribe({next:c=>{this.categories.set(c);this.categoriesLoading.set(false)},error:()=>{this.categoriesError.set('Impossible de charger les catégories.');this.categoriesLoading.set(false)}})}
 submit(){this.form.markAllAsTouched();if(this.form.invalid||this.creating())return;this.creating.set(true);this.createError.set('');const value=this.form.getRawValue();const request:CreateTicketRequest={title:value.title.trim(),description:value.description.trim(),priority:value.priority!,categoryId:value.categoryId};this.ticketService.createTicket(request).subscribe({next:ticket=>void this.router.navigate(['/app/tickets',ticket.id]),error:(e:HttpErrorResponse)=>{this.creating.set(false);this.createError.set(e.status===400?'Les informations saisies sont invalides.':e.status===403?"Vous n'êtes pas autorisé à créer un ticket.":e.status===404?'La catégorie sélectionnée est introuvable.':'Impossible de créer le ticket.') }})}
 cancel(){void this.router.navigate(['/app/tickets'])}
}
