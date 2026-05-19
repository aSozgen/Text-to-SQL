import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../../../api/api-configuration';

import { AdminTemplatesComponent } from './admin-templates.component';

describe('AdminTemplatesComponent', () => {
  let component: AdminTemplatesComponent;
  let fixture: ComponentFixture<AdminTemplatesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminTemplatesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideApiConfiguration('')
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminTemplatesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
