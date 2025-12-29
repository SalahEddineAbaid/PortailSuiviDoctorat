import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AdminRoutingModule } from './admin.routes';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    // Components are standalone, no declarations needed
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    AdminRoutingModule,
    SharedModule
  ]
})
export class AdminModule { }