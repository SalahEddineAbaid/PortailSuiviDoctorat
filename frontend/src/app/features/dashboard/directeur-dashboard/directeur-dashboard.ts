import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-directeur-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './directeur-dashboard.html',
  styleUrl: './directeur-dashboard.scss'
})
export class DirecteurDashboard implements OnInit {

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    console.log('✅ DirecteurDashboard chargé');
  }
}