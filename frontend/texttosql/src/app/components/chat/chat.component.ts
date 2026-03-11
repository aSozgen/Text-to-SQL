import {
  Component, inject, signal, computed, effect, untracked,
  ViewChild, ElementRef, DestroyRef, OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../../api/api';
import { AuthService } from '../../core/auth.service';

// API Models
import { ChatDto } from '../../api/models/chat-dto';
import { MessageDto } from '../../api/models/message-dto';
import { DatabaseDto } from '../../api/models/database-dto';
import { TableDto } from '../../api/models/table-dto';
import { ColumnDto } from '../../api/models/column-dto';
import { ChatSearchResponse } from '../../api/models/chat-search-response';
import { PagedModelChatDto } from '../../api/models/paged-model-chat-dto';
import { PagedModelMessageDto } from '../../api/models/paged-model-message-dto';

// API Functions
import {
  getChats, createChat, deleteChat, updateChat, getMessages, createMessage,
  updateMessageFeedback, deleteMessage, updateMessageContent, searchChat
} from '../../api/functions';
import {SchemaService} from '../../core/schema.service';
import {ErrorHandlerService} from '../../core/error.handler.service';
import { exportChatToCsv, exportChatToJson, exportChatToMarkdown } from '../../api/functions';

type ExportFormat = 'JSON' | 'CSV' | 'Markdown';
type ModalType = 'DELETE_CHAT' | 'DELETE_MSG' | 'RENAME_CHAT' | 'NONE';

type ModalTarget = ChatDto | MessageDto | null;
const MOBILE_BREAKPOINT = 992;
const PAGE_SIZE = 20;

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly authService = inject(AuthService);
  private readonly schemaService = inject(SchemaService);
  private readonly errorHandler = inject(ErrorHandlerService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef<HTMLElement>;

  // --- Layout Signals ---
  isLeftSidebarOpen = signal(true);
  isRightSidebarOpen = signal(true);

  // --- State Signals ---
  isGuest = computed(() => !this.authService.currentUser());
  isLoading = signal(false);
  isSending = signal(false);
  isLoadingMoreMsg = signal(false);

  // Data
  chats = signal<ChatDto[]>([]);
  messages = signal<MessageDto[]>([]);

  // Cache System — used for read-only display; invalidated on mutations.
  private messageCache = new Map<string, MessageDto[]>();
  private chatPaginationState = new Map<string, { page: number; totalPages: number }>();

  // Schema Context
  databases = signal<DatabaseDto[]>([]);
  tables = signal<TableDto[]>([]);
  activeColumns = signal<ColumnDto[]>([]);

  // Pagination & Search
  page = signal<number>(0);
  size = signal<number>(100);
  sort = signal<string>('createdAt');
  direction = signal<'asc' | 'desc'>('desc');
  chatPage = signal(0);
  chatTotalPages = signal(0);
  searchQuery = signal('');
  hasMoreChats = computed(() => this.chatPage() < this.chatTotalPages() - 1);

  currentChatPage = signal(0);
  currentChatTotalPages = signal(0);
  hasMoreMessages = computed(() => this.currentChatPage() < this.currentChatTotalPages() - 1);

  // Selections
  selectedChatId = signal<string | null>(null);
  selectedDbId = signal('');
  selectedTableId = signal('');

  // UI State
  userQuery = signal('');
  editingMessageId = signal<string | null>(null);
  editContent = signal('');
  copiedMessageId = signal<string | null>(null);
  modalMode = signal<ModalType>('NONE');
  targetItem = signal<ModalTarget>(null);
  newNameInput = signal('');
  errorMessage = signal<string | null>(null);

  selectedTableName = computed(() => {
    const tId = this.selectedTableId();
    if (!tId) return null;
    return this.tables().find(t => t.tableId === tId)?.name ?? null;
  });

  constructor() {
    // Initial Load
    effect(() => {
      const isGuest = this.isGuest();
      untracked(() => {
        if (!isGuest) {
          this.loadDatabases();
          this.loadChats(0, true);
        }
        this.checkScreenSize();
      });
    });

    // Chat Selection → Load Messages
    effect(() => {
      const chatId = this.selectedChatId();
      if (chatId) {
        untracked(() => {
          this.loadMessagesForSelectedChat(chatId);
          this.collapseSidebarsOnMobile();
        });
      } else {
        this.messages.set([]);
      }
    });

    // DB Selection → Load Tables
    effect(() => {
      const dbId = this.selectedDbId();
      untracked(() => {
        this.tables.set([]);
        this.selectedTableId.set('');
        this.activeColumns.set([]);
        if (dbId) this.loadTables(dbId);
      });
    });

    // Table Selection → Load Columns
    effect(() => {
      const dbId = this.selectedDbId();
      const tableId = this.selectedTableId();
      if (dbId && tableId) {
        untracked(() => this.loadColumns(dbId, tableId));
      } else {
        this.activeColumns.set([]);
      }
    });
  }

  ngOnInit(): void {
    // Listen for resize to toggle sidebar responsively
    const onResize = () => this.checkScreenSize();
    window.addEventListener('resize', onResize);
    this.destroyRef.onDestroy(() => window.removeEventListener('resize', onResize));
  }

  // --- Layout Methods ---
  toggleLeftSidebar(): void {
    this.isLeftSidebarOpen.update(v => !v);
  }

  toggleRightSidebar(): void {
    this.isRightSidebarOpen.update(v => !v);
  }

  private checkScreenSize(): void {
    if (window.innerWidth <= MOBILE_BREAKPOINT) {
      this.isLeftSidebarOpen.set(false);
      this.isRightSidebarOpen.set(false);
    }
  }

  private collapseSidebarsOnMobile(): void {
    if (window.innerWidth <= MOBILE_BREAKPOINT) {
      this.isLeftSidebarOpen.set(false);
    }
  }

  // --- Scroll Logic ---
  onScroll(event: Event): void {
    const element = event.target as HTMLElement;
    if (element.scrollTop === 0 && this.hasMoreMessages() && !this.isLoadingMoreMsg()) {
      this.loadMoreMessages(element.scrollHeight);
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.scrollContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    }, 100);
  }

  // --- Date Parsing Utility ---
  private parseCustomDate(dateStr: string): number {
    const [datePart, timePart] = dateStr.split(' ');
    if (!datePart || !timePart) return 0;
    const [day, month, year] = datePart.split('.');
    const ts = new Date(`${year}-${month}-${day}T${timePart}`).getTime();
    return isNaN(ts) ? 0 : ts;
  }

  // --- Sorting Logic ---
  private sortMessages(msgs: MessageDto[]): MessageDto[] {
    return [...msgs].sort((a, b) => {
      const timeA = a.createdAt ? this.parseCustomDate(a.createdAt) : 0;
      const timeB = b.createdAt ? this.parseCustomDate(b.createdAt) : 0;
      if (timeA !== timeB) return timeA - timeB;
      // Same timestamp: USER before LLM
      if (a.senderType === 'USER' && b.senderType !== 'USER') return -1;
      if (a.senderType !== 'USER' && b.senderType === 'USER') return 1;
      return 0;
    });
  }

  // --- Data Loading Methods ---
  async loadDatabases(): Promise<void> {
    try {
      const params = { page: this.page(), size: this.size(), sort: this.sort(), direction: this.direction() };
      this.databases.set(await this.schemaService.loadDatabases(null));
    } catch (e) {
      console.error('Failed to load databases', e);
    }
  }

  async loadTables(dbId: string): Promise<void> {
    try {
      this.tables.set(await this.schemaService.loadTables(dbId));
    } catch (e) {
      console.error('Failed to load tables', e);
    }
  }

  async loadColumns(dbId: string, tableId: string): Promise<void> {
    try {
      this.activeColumns.set(await this.schemaService.loadColumns(dbId, tableId));
    } catch (e) {
      console.error('Failed to load columns', e);
    }
  }

  // --- Chat List Logic ---
  async onSearch(): Promise<void> {
    this.chatPage.set(0);
    await this.loadChats(0, true);
  }

  async loadChats(page: number, reset = false): Promise<void> {
    const query = this.searchQuery().trim();
    try {
      let content: ChatDto[] = [];
      let totalPages = 0;

      if (query) {
        const res = await this.api.invoke(searchChat, {
          query, page, size: PAGE_SIZE, sort: 'createdAt', direction: 'desc'
        }) as ChatSearchResponse;
        content = res.chats || [];
        totalPages = content.length < PAGE_SIZE ? page + 1 : page + 2;
      } else {
        const res = await this.api.invoke(getChats, {
          page, size: PAGE_SIZE, sort: 'createdAt', direction: 'desc'
        }) as PagedModelChatDto;
        content = res.content || [];
        totalPages = res.page?.totalPages || 0;
      }

      this.chatTotalPages.set(totalPages);
      if (reset) {
        this.chats.set(content);
      } else {
        this.chats.update(c => [...c, ...content]);
      }
    } catch (e) {
      this.showError(this.errorHandler.message(e));
    }
  }

  loadMoreChats(): void {
    if (this.hasMoreChats()) {
      this.chatPage.update(p => p + 1);
      this.loadChats(this.chatPage());
    }
  }

  // --- Message Logic ---
  async loadMessagesForSelectedChat(chatId: string): Promise<void> {
    if (this.messageCache.has(chatId)) {
      this.messages.set(this.messageCache.get(chatId)!);
      const state = this.chatPaginationState.get(chatId);
      if (state) {
        this.currentChatPage.set(state.page);
        this.currentChatTotalPages.set(state.totalPages);
      }
      this.scrollToBottom();
      return;
    }
    this.currentChatPage.set(0);
    await this.fetchMessages(chatId, 0, true);
  }

  async forceReloadChatMessages(chatId: string): Promise<void> {
    this.messageCache.delete(chatId);
    this.chatPaginationState.delete(chatId);
    this.currentChatPage.set(0);
    await this.fetchMessages(chatId, 0, true);
  }

  async loadMoreMessages(prevHeight: number): Promise<void> {
    if (!this.hasMoreMessages()) return;

    const nextPage = this.currentChatPage() + 1;
    this.currentChatPage.set(nextPage);
    await this.fetchMessages(this.selectedChatId()!, nextPage, false);

    setTimeout(() => {
      const el = this.scrollContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight - prevHeight;
      }
    }, 0);
  }

  async fetchMessages(chatId: string, page: number, isInitialLoad: boolean): Promise<void> {
    if (isInitialLoad) this.isLoading.set(true);
    else this.isLoadingMoreMsg.set(true);

    try {
      const res = await this.api.invoke(getMessages, {
        chatID: chatId, page, size: PAGE_SIZE
      }) as PagedModelMessageDto;

      const fetchedMessages = this.sortMessages(res.content || []);
      const totalPages = res.page?.totalPages || 0;
      this.currentChatTotalPages.set(totalPages);

      let updatedList: MessageDto[];
      if (isInitialLoad) {
        updatedList = fetchedMessages;
      } else {
        updatedList = this.sortMessages([...fetchedMessages, ...this.messages()]);
      }

      // Update cache & pagination state outside signal callback
      this.messageCache.set(chatId, updatedList);
      this.chatPaginationState.set(chatId, {
        page: this.currentChatPage(),
        totalPages: this.currentChatTotalPages()
      });

      this.messages.set(updatedList);

      if (isInitialLoad) this.scrollToBottom();
    } catch (e) {
      this.showError(this.errorHandler.message(e));
    } finally {
      this.isLoading.set(false);
      this.isLoadingMoreMsg.set(false);
    }
  }

  // --- Core Actions ---
  createNewChat(): void {
    this.selectedChatId.set(null);
    this.messages.set([]);
    this.errorMessage.set(null);
    this.collapseSidebarsOnMobile();
  }

  selectChat(chatId: string): void {
    this.selectedChatId.set(chatId);
    this.errorMessage.set(null);
  }

  async sendMessage(query: string): Promise<void> {
    this.errorMessage.set(null);
    const trimmed = query.trim();
    if (!trimmed) return;

    this.isSending.set(true);
    let chatId = this.selectedChatId();

    // Auto-create chat if none selected
    if (!chatId) {
      const newChatName = trimmed.length > 30 ? `${trimmed.substring(0, 30)}...` : trimmed;
      try {
        const chatRes = await this.api.invoke(createChat, { body: { name: newChatName } }) as ChatDto;
        chatId = chatRes.chatId!;
        this.selectedChatId.set(chatId);
        this.chats.update(c => [chatRes, ...c]);
      } catch {
        this.showError('Failed to create chat session.');
        this.isSending.set(false);
        return;
      }
    }

    // Optimistic user message
    const tempId = crypto.randomUUID();
    const userMsg: MessageDto = {
      messageId: tempId,
      content: trimmed,
      senderType: 'USER',
      createdAt: this.getCurrentFormattedDate(),
      databaseId: this.selectedDbId() || undefined
    };

    this.appendLocalMessage(chatId!, userMsg);
    this.userQuery.set('');
    this.scrollToBottom();

    try {
      await this.api.invoke(createMessage, {
        chatID: chatId!,
        body: { content: trimmed, databaseId: this.selectedDbId() || undefined }
      });
      // Reload to get both the real user message and the LLM response
      await this.forceReloadChatMessages(chatId!);
    } catch {
      const errorMsg: MessageDto = {
        messageId: crypto.randomUUID(),
        content: '-- Error generating SQL. Please try again.',
        senderType: 'LLM',
        createdAt: this.getCurrentFormattedDate()
      };
      this.appendLocalMessage(chatId!, errorMsg);
      this.scrollToBottom();
    } finally {
      this.isSending.set(false);
    }
  }

  private getCurrentFormattedDate(): string {
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(now.getDate())}.${pad(now.getMonth() + 1)}.${now.getFullYear()} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  }

  private appendLocalMessage(chatId: string, msg: MessageDto): void {
    this.messages.update(m => this.sortMessages([...m, msg]));
    // Keep cache in sync
    const cached = this.messageCache.get(chatId);
    if (cached) {
      this.messageCache.set(chatId, this.sortMessages([...cached, msg]));
    }
  }

  // --- Modal Logic ---
  openDeleteChatModal(chat: ChatDto, event: Event): void {
    event.stopPropagation();
    this.targetItem.set(chat);
    this.modalMode.set('DELETE_CHAT');
  }

  openDeleteMessageModal(msg: MessageDto): void {
    if (msg.senderType !== 'USER') return;
    this.targetItem.set(msg);
    this.modalMode.set('DELETE_MSG');
  }

  openRenameChatModal(chat: ChatDto, event: Event): void {
    event.stopPropagation();
    this.targetItem.set(chat);
    this.newNameInput.set(chat.name);
    this.modalMode.set('RENAME_CHAT');
  }

  closeModal(): void {
    this.modalMode.set('NONE');
    this.targetItem.set(null);
    this.newNameInput.set('');
  }

  async confirmAction(): Promise<void> {
    const mode = this.modalMode();
    const item = this.targetItem();
    const chatId = this.selectedChatId();
    this.isLoading.set(true);

    try {
      switch (mode) {
        case 'DELETE_CHAT': {
          const chat = item as ChatDto;
          if (!chat?.chatId) throw new Error('Invalid item');
          await this.api.invoke(deleteChat, { chatID: chat.chatId });
          this.chats.update(c => c.filter(x => x.chatId !== chat.chatId));
          this.messageCache.delete(chat.chatId);
          if (chatId === chat.chatId) this.createNewChat();
          break;
        }
        case 'DELETE_MSG': {
          const msg = item as MessageDto;
          if (!msg?.messageId || !chatId) throw new Error('Invalid item');
          await this.api.invoke(deleteMessage, { chatID: chatId, messageID: msg.messageId });
          await this.forceReloadChatMessages(chatId);
          break;
        }
        case 'RENAME_CHAT': {
          const chat = item as ChatDto;
          if (!chat?.chatId) throw new Error('Invalid item');
          const newName = this.newNameInput().trim();
          if (newName && newName !== chat.name) {
            await this.api.invoke(updateChat, { chatID: chat.chatId, body: { name: newName } });
            this.chats.update(c =>
              c.map(x => x.chatId === chat.chatId ? { ...x, name: newName } : x)
            );
          }
          break;
        }
      }
      this.closeModal();
    } catch {
      this.closeModal();
      this.showError('Operation failed. Please try again.');
    } finally {
      this.isLoading.set(false);
    }
  }

  // --- Inline Edit ---
  startEditMessage(msg: MessageDto): void {
    if (msg.senderType !== 'USER') return;
    this.editingMessageId.set(msg.messageId!);
    this.editContent.set(msg.content);
  }

  cancelEdit(): void {
    this.editingMessageId.set(null);
    this.editContent.set('');
  }

  async saveEditMessage(msg: MessageDto): Promise<void> {
    if (msg.senderType !== 'USER') return;
    const newContent = this.editContent().trim();
    if (!newContent || newContent === msg.content) {
      this.cancelEdit();
      return;
    }

    const chatId = this.selectedChatId();
    if (!chatId || !msg.messageId) {
      this.showError('Invalid state for editing.');
      return;
    }

    this.cancelEdit();
    this.isLoading.set(true);

    try {
      await this.api.invoke(updateMessageContent, {
        chatID: chatId, messageID: msg.messageId,
        body: { content: newContent, databaseId: msg.databaseId }
      });
      await this.forceReloadChatMessages(chatId);
    } catch {
      this.showError('Failed to update message.');
    } finally {
      this.isLoading.set(false);
    }
  }

  // --- Feedback ---
  async sendFeedback(msg: MessageDto, type: 'GOOD' | 'BAD'): Promise<void> {
    const chatId = this.selectedChatId();
    if (!chatId || !msg.messageId) return;

    const newFeedback: 'GOOD' | 'BAD' | 'NONE' = msg.feedback === type ? 'NONE' : type;
    const previousFeedback = msg.feedback;

    // Optimistic update (safe — feedback doesn't change IDs)
    const updateFn = (m: MessageDto): MessageDto =>
      m.messageId === msg.messageId ? { ...m, feedback: newFeedback } : m;

    this.messages.update(msgs => msgs.map(updateFn));
    const cached = this.messageCache.get(chatId);
    if (cached) {
      this.messageCache.set(chatId, cached.map(updateFn));
    }

    try {
      await this.api.invoke(updateMessageFeedback, {
        chatID: chatId, messageID: msg.messageId, body: { feedback: newFeedback }
      });
    } catch {
      // Revert on failure
      const revertFn = (m: MessageDto): MessageDto =>
        m.messageId === msg.messageId ? { ...m, feedback: previousFeedback } : m;
      this.messages.update(msgs => msgs.map(revertFn));
      if (cached) {
        this.messageCache.set(chatId, cached.map(revertFn));
      }
    }
  }

  // --- Export ---
  async exportChat(format: ExportFormat): Promise<void> {
    const chatId = this.selectedChatId();
    if (!chatId) {
      this.showError('No chat selected.');
      return;
    }

    const chatName = this.chats().find(c => c.chatId === chatId)?.name || 'export';

    const config: Record<ExportFormat, { fn: any; ext: string; mime: string }> = {
      CSV:  { fn: exportChatToCsv,      ext: 'csv', mime: 'text/csv' },
      JSON: { fn: exportChatToJson,     ext: 'json', mime: 'application/json' },
      Markdown:  { fn: exportChatToMarkdown, ext: 'md',  mime: 'text/markdown' },
    };

    const { fn, ext, mime } = config[format];

    try {
      const response = await this.api.invoke(fn, { chatID: chatId });

      const blob = response instanceof Blob
        ? response
        : new Blob([response as string], { type: mime });

      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${chatName}.${ext}`;
      a.click();
      URL.revokeObjectURL(url);

    } catch (e) {
      this.showError(this.errorHandler.message(e));
    }
  }
  copySQL(messageId: string, sql: string): void {
    navigator.clipboard.writeText(sql).then(() => {
      this.copiedMessageId.set(messageId);
      setTimeout(() => {
        if (this.copiedMessageId() === messageId) {
          this.copiedMessageId.set(null);
        }
      }, 2000);
    });
  }

  showError(msg: string): void {
    this.errorMessage.set(msg);
  }

  closeErrorModal(): void {
    this.errorMessage.set(null);
  }
}
