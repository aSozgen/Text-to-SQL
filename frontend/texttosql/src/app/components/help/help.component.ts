import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { contactSupport } from '../../api/functions';
import { ErrorHandlerService } from '../../core/error.handler.service';

interface FaqItem {
  question: string;
  answer: string;
  expanded?: boolean;
}

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './help.component.html',
  styleUrl: './help.component.scss'
})
export class HelpComponent {
  private fb = inject(FormBuilder);
  private api = inject(Api);
  private errorHandler = inject(ErrorHandlerService);

  contactForm: FormGroup;
  isSubmitting = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  faqs: FaqItem[] = [
    {
      question: 'Does QueryGen execute the generated SQL queries?',
      answer: 'No. The system is designed solely for converting natural language to SQL. It does not execute queries on any database for security reasons. You should review and run the code in your own environment.',
      expanded: false
    },
    {
      question: 'Which databases and SQL dialects are supported?',
      answer: 'The system is optimized for PostgreSQL but generates standard ANSI SQL, which is largely compatible with MySQL, SQLite, and SQL Server.',
      expanded: false
    },
    {
      question: 'How do I ensure the SQL accuracy is high?',
      answer: 'For the best results, make sure you have imported your database schema (tables and columns) in the "Schemas" tab. Providing schema context significantly reduces hallucinations.',
      expanded: false
    },
    {
      question: 'Is my database data stored on your servers?',
      answer: 'No. We only store the schema structure (metadata like table names and types) to provide context to the LLM. We never access or store your actual row data.',
      expanded: false
    },
    {
      question: 'Can I use QueryGen without a schema?',
      answer: 'Yes, you can use "Schema-Blind" mode for general SQL questions, but "Schema-Aware" mode is recommended for database-specific queries.',
      expanded: false
    }
  ];

  constructor() {
    this.contactForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      subject: ['', [Validators.required, Validators.minLength(5)]],
      message: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  get f() { return this.contactForm.controls; }

  toggleFaq(index: number) {
    this.faqs[index].expanded = !this.faqs[index].expanded;
  }

  async onSubmit() {
    this.successMessage.set(null);
    this.errorMessage.set(null);

    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    try {
      await this.api.invoke(contactSupport, {
        body: this.contactForm.value
      });

      this.successMessage.set('Message sent successfully! We will get back to you soon.');
      this.contactForm.reset();
      this.faqs.forEach(f => f.expanded = false);

      setTimeout(() => this.successMessage.set(null), 5000);
    } catch (e: any) {
      console.error('Failed to send support message', e);
      this.errorMessage.set(this.errorHandler.message(e));
      setTimeout(() => this.errorMessage.set(null), 5000);
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
