import { Component, inject, signal, computed, effect, untracked, ViewChild, ElementRef } from '@angular/core';
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
import { FeedbackRequest } from '../../api/models/feedback-request';
import { ChatSearchResponse } from '../../api/models/chat-search-response';
import { PagedModelChatDto } from '../../api/models/paged-model-chat-dto';
import { PagedModelMessageDto } from '../../api/models/paged-model-message-dto';

// API Functions
import {
  getChats, createChat, deleteChat, updateChat,
  getMessages, createMessage, updateMessageFeedback, deleteMessage, updateMessageContent,
  getDatabases, getTables, getColumns, searchChat
} from '../../api/functions';

type ExportFormat = 'JSON' | 'CSV' | 'TXT';
type ModalType = 'DELETE_CHAT' | 'DELETE_MSG' | 'RENAME_CHAT' | 'NONE';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent {
  private readonly api = inject(Api);
  private readonly authService = inject(AuthService);

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  // --- Layout Signals ---
  isLeftSidebarOpen = signal<boolean>(true);
  isRightSidebarOpen = signal<boolean>(true);

  // --- State Signals ---
  isGuest = computed(() => !this.authService.currentUser());
  isLoading = signal<boolean>(false);
  isSending = signal<boolean>(false);
  isLoadingMoreMsg = signal<boolean>(false);

  // Data
  chats = signal<ChatDto[]>([]);
  messages = signal<MessageDto[]>([]);

  // Cache System
  // Cache'i sadece "Görüntüleme" için kullanacağız.
  // Update/Delete işlemlerinde ilgili chatin cache'i silinmeli.
  private messageCache = new Map<string, MessageDto[]>();
  private chatPaginationState = new Map<string, { page: number, totalPages: number }>();

  // Schema Context
  databases = signal<DatabaseDto[]>([]);
  tables = signal<TableDto[]>([]);
  activeColumns = signal<ColumnDto[]>([]);

  // Pagination & Search
  chatPage = signal<number>(0);
  chatTotalPages = signal<number>(0);
  searchQuery = signal<string>('');
  hasMoreChats = computed(() => this.chatPage() < this.chatTotalPages() - 1);

  currentChatPage = signal<number>(0);
  currentChatTotalPages = signal<number>(0);
  hasMoreMessages = computed(() => this.currentChatPage() < this.currentChatTotalPages() - 1);

  // Selections
  selectedChatId = signal<string | null>(null);
  selectedDbId = signal<string>('');
  selectedTableId = signal<string>('');

  // UI State
  userQuery = signal<string>('');
  editingMessageId = signal<string | null>(null);
  editContent = signal<string>('');
  copiedMessageId = signal<string | null>(null);
  modalMode = signal<ModalType>('NONE');
  targetItem = signal<any>(null);
  newNameInput = signal<string>('');
  errorMessage = signal<string | null>(null);

  selectedTableName = computed(() => {
    const tId = this.selectedTableId();
    if (!tId) return null;
    const found = this.tables().find(t => t.tableId === tId);
    return found ? found.name : null;
  });

  constructor() {
    // Initial Load
    effect(() => {
      const _ = this.isGuest();
      untracked(() => {
        this.loadDatabases();
        this.loadChats(0, true);
        this.checkScreenSize();
      });
    });

    // Chat Selection -> Load Messages
    effect(() => {
      const chatId = this.selectedChatId();
      if (chatId) {
        untracked(() => {
          this.loadMessagesForSelectedChat(chatId);
          if (window.innerWidth <= 992) {
            this.isLeftSidebarOpen.set(false);
          }
        });
      } else {
        this.messages.set([]);
      }
    });

    // DB & Table Loaders
    effect(() => {
      const dbId = this.selectedDbId();
      untracked(() => {
        this.tables.set([]);
        this.selectedTableId.set('');
        this.activeColumns.set([]);
        if (dbId) this.loadTables(dbId);
      });
    });

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

  // --- Layout Methods ---
  toggleLeftSidebar() { this.isLeftSidebarOpen.update(v => !v); }
  toggleRightSidebar() { this.isRightSidebarOpen.update(v => !v); }
  private checkScreenSize() { if (window.innerWidth <= 992) { this.isLeftSidebarOpen.set(false); this.isRightSidebarOpen.set(false); } }

  // --- Scroll Logic ---
  onScroll(event: Event) {
    const element = event.target as HTMLElement;
    if (element.scrollTop === 0 && this.hasMoreMessages() && !this.isLoadingMoreMsg()) {
      const currentScrollHeight = element.scrollHeight;
      this.loadMoreMessages(currentScrollHeight);
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      try {
        if (this.scrollContainer) {
          this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
        }
      } catch(err) { }
    }, 100);
  }

  // --- Sorting Logic ---
  private sortMessages(msgs: MessageDto[]): MessageDto[] {
    return msgs.sort((a, b) => {
      if (!a.createdAt || !b.createdAt) return 0;
      const parseDate = (dateStr: string) => {
        const [datePart, timePart] = dateStr.split(' ');
        const [day, month, year] = datePart.split('.');
        return new Date(`${year}-${month}-${day}T${timePart}`).getTime();
      };
      const timeA = parseDate(a.createdAt);
      const timeB = parseDate(b.createdAt);
      if (timeA !== timeB) return timeA - timeB;
      if (a.senderType === 'USER' && b.senderType !== 'USER') return -1;
      if (a.senderType !== 'USER' && b.senderType === 'USER') return 1;
      return 0;
    });
  }

  // --- Data Loading Methods ---
  async loadDatabases() {
    if (this.isGuest()) return;
    try {
      const res = await this.api.invoke(getDatabases, { page: 0, size: 100 }) as any;
      this.databases.set(res.content || []);
    } catch (e) { }
  }

  async loadTables(dbId: string) {
    try {
      const res = await this.api.invoke(getTables, { databaseId: dbId }) as any;
      this.tables.set(Array.isArray(res) ? res : (res.content || []));
    } catch (e) { }
  }

  async loadColumns(dbId: string, tableId: string) {
    try {
      const res = await this.api.invoke(getColumns, { databaseId: dbId, tableId: tableId }) as any;
      this.activeColumns.set(Array.isArray(res) ? res : (res.content || []));
    } catch (e) { }
  }

  // --- Chat List Logic ---
  async onSearch() {
    this.chatPage.set(0);
    this.loadChats(0, true);
  }

  async loadChats(page: number, reset: boolean = false) {
    const query = this.searchQuery().trim();
    const size = 20;
    try {
      let content: ChatDto[] = [];
      let totalPages = 0;
      if (query) {
        const res = await this.api.invoke(searchChat, {
          query: query, page: page, size: size, sort: 'createdAt', direction: 'desc'
        }) as ChatSearchResponse;
        content = res.chats || [];
        totalPages = content.length < size ? page + 1 : page + 2;
      } else {
        const res = await this.api.invoke(getChats, {
          page: page, size: size, sort: 'createdAt', direction: 'desc'
        }) as PagedModelChatDto;
        content = res.content || [];
        totalPages = res.page?.totalPages || 0;
      }
      this.chatTotalPages.set(totalPages);
      if (reset) this.chats.set(content);
      else this.chats.update(c => [...c, ...content]);
    } catch (e) { this.showError(this.getErrorMessage(e)); }
  }

  loadMoreChats() {
    if (this.hasMoreChats()) {
      this.chatPage.update(p => p + 1);
      this.loadChats(this.chatPage());
    }
  }

  // --- Message Logic ---
  async loadMessagesForSelectedChat(chatId: string) {
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

  // FORCE RELOAD: Cache'i temizleyip sunucudan taze veri çeker
  async forceReloadChatMessages(chatId: string) {
    this.messageCache.delete(chatId);
    this.chatPaginationState.delete(chatId);
    this.currentChatPage.set(0);
    await this.fetchMessages(chatId, 0, true);
  }

  async loadMoreMessages(prevHeight: number) {
    if (this.hasMoreMessages()) {
      const nextPage = this.currentChatPage() + 1;
      this.currentChatPage.set(nextPage);
      await this.fetchMessages(this.selectedChatId()!, nextPage, false);
      setTimeout(() => {
        if (this.scrollContainer) {
          const newHeight = this.scrollContainer.nativeElement.scrollHeight;
          this.scrollContainer.nativeElement.scrollTop = newHeight - prevHeight;
        }
      }, 0);
    }
  }

  async fetchMessages(chatId: string, page: number, isInitialLoad: boolean) {
    if (isInitialLoad) this.isLoading.set(true);
    else this.isLoadingMoreMsg.set(true);
    try {
      const res = await this.api.invoke(getMessages, {
        chatID: chatId, page: page, size: 20
      }) as PagedModelMessageDto;
      let fetchedMessages = res.content || [];
      fetchedMessages = this.sortMessages(fetchedMessages);
      this.currentChatTotalPages.set(res.page?.totalPages || 0);
      this.messages.update(current => {
        let updatedList: MessageDto[];
        if (isInitialLoad) {
          updatedList = fetchedMessages;
          this.scrollToBottom();
        } else {
          updatedList = [...fetchedMessages, ...current];
          updatedList = this.sortMessages(updatedList);
        }
        this.messageCache.set(chatId, updatedList);
        this.chatPaginationState.set(chatId, {
          page: this.currentChatPage(), totalPages: this.currentChatTotalPages()
        });
        return updatedList;
      });
    } catch (e) { this.showError(this.getErrorMessage(e)); }
    finally { this.isLoading.set(false); this.isLoadingMoreMsg.set(false); }
  }

  // --- Core Actions ---
  createNewChat() {
    this.selectedChatId.set(null);
    this.messages.set([]);
    this.errorMessage.set(null);
    if (window.innerWidth <= 992) this.isLeftSidebarOpen.set(false);
  }

  selectChat(chatId: string) {
    this.selectedChatId.set(chatId);
    this.errorMessage.set(null);
  }

  async sendMessage(query: string) {
    this.errorMessage.set(null);
    const trimmed = query.trim();
    if (!trimmed) return;
    this.isSending.set(true);
    let chatId = this.selectedChatId();
    if (!chatId) {
      const newChatName = trimmed.length > 30 ? trimmed.substring(0, 30) + '...' : trimmed;
      try {
        const chatRes = await this.api.invoke(createChat, { body: { name: newChatName } }) as ChatDto;
        chatId = chatRes.chatId!;
        this.selectedChatId.set(chatId);
        this.chats.update(c => [chatRes, ...c]);
      } catch (e) {
        this.showError("Failed to create chat session.");
        this.isSending.set(false);
        return;
      }
    }
    const tempId = crypto.randomUUID();
    const userMsg: MessageDto = {
      messageId: tempId, content: trimmed, senderType: 'USER',
      createdAt: this.getCurrentFormattedDate(), databaseId: this.selectedDbId() || undefined
    };

    // Optimistic Update (Ekleme için güvenli)
    this.updateLocalMessages(chatId!, userMsg);
    this.userQuery.set('');
    this.scrollToBottom();

    try {
      const response = await this.api.invoke(createMessage, {
        chatID: chatId!, body: { content: trimmed, databaseId: this.selectedDbId() || undefined }
      }) as MessageDto;

      // Gerçek cevabı ekle. Backend yeni bir ID ürettiği için,
      // yukarıdaki tempId'li mesajı silip yenisini koymak yerine,
      // listenin en altına ekliyoruz.
      // Düzgün bir yapı için aslında tempId'li mesajı listeden çıkarıp
      // backend'den gelen 2 mesajı (User + LLM) eklemek en doğrusudur.
      // Ancak basitlik adına şimdilik böyle:

      // Cache'i temizleyip yeniden yüklemek en garantisi:
      await this.forceReloadChatMessages(chatId!);

    } catch (e) {
      const errorMsg: MessageDto = {
        messageId: crypto.randomUUID(), content: "-- Error generating SQL. Please try again.",
        senderType: 'LLM', createdAt: this.getCurrentFormattedDate()
      };
      this.updateLocalMessages(chatId!, errorMsg);
      this.scrollToBottom();
    } finally { this.isSending.set(false); }
  }

  private getCurrentFormattedDate(): string {
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(now.getDate())}.${pad(now.getMonth() + 1)}.${now.getFullYear()} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
  }

  private updateLocalMessages(chatId: string, msg: MessageDto) {
    this.messages.update(m => { const newList = [...m, msg]; return this.sortMessages(newList); });
    // Cache güncelle
    if (this.messageCache.has(chatId)) {
      const cached = this.messageCache.get(chatId)!;
      this.messageCache.set(chatId, this.sortMessages([...cached, msg]));
    }
  }

  // --- Modal Logic ---
  openDeleteChatModal(chat: ChatDto, event: Event) { event.stopPropagation(); this.targetItem.set(chat); this.modalMode.set('DELETE_CHAT'); }
  openDeleteMessageModal(msg: MessageDto) { if (msg.senderType !== 'USER') return; this.targetItem.set(msg); this.modalMode.set('DELETE_MSG'); }
  openRenameChatModal(chat: ChatDto, event: Event) { event.stopPropagation(); this.targetItem.set(chat); this.newNameInput.set(chat.name); this.modalMode.set('RENAME_CHAT'); }
  closeModal() { this.modalMode.set('NONE'); this.targetItem.set(null); this.newNameInput.set(''); }

  async confirmAction() {
    const mode = this.modalMode();
    const item = this.targetItem();
    const chatId = this.selectedChatId();
    this.isLoading.set(true);

    try {
      if (mode === 'DELETE_CHAT') {
        if (!item?.chatId) throw new Error("Invalid item");
        await this.api.invoke(deleteChat, { chatID: item.chatId });

        // Success
        this.chats.update(c => c.filter(x => x.chatId !== item.chatId));
        this.messageCache.delete(item.chatId);
        if (chatId === item.chatId) this.createNewChat();
      }
      else if (mode === 'DELETE_MSG') {
        if (!item?.messageId || !chatId) throw new Error("Invalid item");

        await this.api.invoke(deleteMessage, { chatID: chatId, messageID: item.messageId });

        // Success: Cache'i temizle ve yeniden yükle (Tutarlılık için en iyisi)
        // Optimistic update yapmıyoruz çünkü ID'ler kayıyor.
        await this.forceReloadChatMessages(chatId);
      }
      else if (mode === 'RENAME_CHAT') {
        if (!item?.chatId) throw new Error("Invalid item");
        const newName = this.newNameInput().trim();
        if (newName && newName !== item.name) {
          await this.api.invoke(updateChat, { chatID: item.chatId, body: { name: newName } });
          this.chats.update(c => c.map(x => x.chatId === item.chatId ? { ...x, name: newName } : x));
        }
      }
      this.closeModal();
    } catch (e) {
      this.closeModal();
      this.showError("Operation failed. Please try again.");
    }
    finally { this.isLoading.set(false); }
  }

  // --- Inline Edit ---
  startEditMessage(msg: MessageDto) { if (msg.senderType !== 'USER') return; this.editingMessageId.set(msg.messageId!); this.editContent.set(msg.content); }
  cancelEdit() { this.editingMessageId.set(null); this.editContent.set(''); }

  async saveEditMessage(msg: MessageDto) {
    if (msg.senderType !== 'USER') return;
    const newContent = this.editContent().trim();
    if (!newContent || newContent === msg.content) { this.cancelEdit(); return; }

    this.cancelEdit();
    this.isLoading.set(true);

    try {
      if (!this.selectedChatId() || !msg.messageId) throw new Error("Invalid state");

      await this.api.invoke(updateMessageContent, {
        chatID: this.selectedChatId()!, messageID: msg.messageId!,
        body: { content: newContent, databaseId: msg.databaseId }
      });

      // Success: Force Reload to get new IDs
      await this.forceReloadChatMessages(this.selectedChatId()!);

    } catch (e) {
      this.showError("Failed to update message.");
      this.isLoading.set(false); // Hata durumunda loading kapat
    }
  }

  // --- Feedback & Utilities ---
  async sendFeedback(msg: MessageDto, type: 'GOOD' | 'BAD') {
    if (!this.selectedChatId() || !msg.messageId) return;
    const newFeedback: 'GOOD' | 'BAD' | 'NONE' = msg.feedback === type ? 'NONE' : type;

    // Feedback ID değiştirmediği için Optimistic update güvenlidir
    const updateFn = (m: MessageDto) => m.messageId === msg.messageId ? { ...m, feedback: newFeedback } : m;
    this.messages.update(msgs => msgs.map(updateFn));

    // Cache'i de güncelle
    if (this.messageCache.has(this.selectedChatId()!)) {
      const cached = this.messageCache.get(this.selectedChatId()!)!;
      this.messageCache.set(this.selectedChatId()!, cached.map(updateFn));
    }

    try {
      await this.api.invoke(updateMessageFeedback, {
        chatID: this.selectedChatId()!, messageID: msg.messageId, body: { feedback: newFeedback }
      });
    } catch (e) {
      // Revert UI on failure
      const revertFn = (m: MessageDto) => m.messageId === msg.messageId ? { ...m, feedback: msg.feedback } : m;
      this.messages.update(msgs => msgs.map(revertFn));
    }
  }

  exportChat(format: ExportFormat) {
    const chatId = this.selectedChatId();
    if (!chatId || this.messages().length === 0) { this.showError("Nothing to export."); return; }
    const chatName = this.chats().find(c => c.chatId === chatId)?.name || 'export';
    const data = this.messages();
    let content = '', mime = '', ext = '';
    if (format === 'JSON') {
      content = JSON.stringify(data.map(({ messageId, ...r }) => r), null, 2); mime = 'application/json'; ext = 'json';
    } else if (format === 'CSV') {
      content = 'Sender,Time,Content\n' + data.map(m => `${m.senderType},${m.createdAt},"${m.content.replace(/"/g, '""')}"`).join('\n'); mime = 'text/csv'; ext = 'csv';
    } else {
      content = data.map(m => `[${m.createdAt}] ${m.senderType}:\n${m.content}\n---`).join('\n'); mime = 'text/plain'; ext = 'txt';
    }
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([content], { type: mime }));
    a.download = `${chatName}.${ext}`;
    a.click();
  }

  copySQL(messageId: string, sql: string) {
    navigator.clipboard.writeText(sql).then(() => {
      this.copiedMessageId.set(messageId);
      setTimeout(() => { if (this.copiedMessageId() === messageId) this.copiedMessageId.set(null); }, 2000);
    });
  }

  showError(msg: string) { this.errorMessage.set(msg); }
  closeErrorModal() { this.errorMessage.set(null); }

  private getErrorMessage(err: any): string {
    if (err && err.error && typeof err.error === 'object') return err.error.message || err.error.error || "An error occurred.";
    if (typeof err === 'string') return err;
    return "An unexpected error occurred.";
  }
}
