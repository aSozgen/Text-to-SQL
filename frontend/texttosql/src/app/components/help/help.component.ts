import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

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

  contactForm: FormGroup;
  isSubmitting = false;
  successMessage: string | null = null;

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
    this.successMessage = null;

    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    setTimeout(() => {
      this.isSubmitting = false;
      this.successMessage = 'Message sent successfully! We will get back to you soon.';
      this.contactForm.reset();

      this.faqs.forEach(f => f.expanded = false);

      setTimeout(() => this.successMessage = null, 5000);
    }, 1500);
  }
}
