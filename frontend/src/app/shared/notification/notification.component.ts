import { Component, OnInit, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { trigger, style, transition, animate } from '@angular/animations';
import { ErrorHandlerService, AppError } from '../../services/error-handler.service';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss'],
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ 
          transform: 'translateX(100%)', 
          opacity: 0,
          height: '0',
          marginBottom: '0',
          padding: '0'
        }),
        animate('300ms ease-out', style({ 
          transform: 'translateX(0)', 
          opacity: 1,
          height: '*',
          marginBottom: '*',
          padding: '*'
        }))
      ]),
      transition(':leave', [
        style({ 
          transform: 'translateX(0)', 
          opacity: 1 
        }),
        animate('250ms ease-in', style({ 
          transform: 'translateX(100%)', 
          opacity: 0,
          height: '0',
          marginBottom: '0',
          padding: '0'
        }))
      ])
    ])
  ]
})
export class NotificationComponent implements OnInit {
  private errorHandler = inject(ErrorHandlerService);
  
  errors$: Observable<AppError[]>;

  private iconMap: Record<string, string> = {
    'error': 'fas fa-exclamation-circle',
    'warning': 'fas fa-exclamation-triangle',
    'info': 'fas fa-info-circle',
    'success': 'fas fa-check-circle'
  };

  private autoDismissTimeMap: Record<string, number> = {
    'error': 0, // No auto-dismiss
    'warning': 0, // No auto-dismiss
    'info': 5000, // 5 seconds
    'success': 3000 // 3 seconds
  };

  constructor() {
    this.errors$ = this.errorHandler.errors$;
  }

  ngOnInit(): void {
    // Subscribe to errors and initialize component
    this.errors$ = this.errorHandler.errors$;
  }

  dismissError(errorId: string): void {
    this.errorHandler.dismissError(errorId);
  }

  getIconClass(type: string): string {
    return this.iconMap[type] || 'fas fa-info-circle';
  }

  getAutoDismissTime(type: string): number {
    return this.autoDismissTimeMap[type] || 0;
  }

  trackByErrorId(index: number, error: AppError): string {
    return error.id;
  }
}
