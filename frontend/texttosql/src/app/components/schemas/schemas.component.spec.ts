import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../../api/api-configuration';

import { SchemasComponent } from './schemas.component';

describe('SchemasComponent', () => {
  let component: SchemasComponent;
  let fixture: ComponentFixture<SchemasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SchemasComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideApiConfiguration('')
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SchemasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
