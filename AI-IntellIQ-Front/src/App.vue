<template>
  <div class="app">
    <!-- 头部标题 -->
    <div class="app-header">
      <h1 class="app-title">AI 编程小助手</h1>
      <div class="app-subtitle">帮助您解答编程学习和求职面试相关问题</div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-container">
      <!-- 左侧：聊天记录列表 -->
      <div class="sidebar-left">
        <div class="sidebar-header">
          <h3>聊天记录</h3>
          <button class="new-chat-btn" @click="startNewChat">
            <span class="icon">+</span>
            新对话
          </button>
        </div>
        
        <div class="chat-history">
          <div 
            v-for="chat in chatHistory" 
            :key="chat.id"
            :class="['chat-item', { active: chat.id === currentChatId }]"
            @click="switchToChat(chat.id)"
          >
            <div class="chat-item-content">
              <div class="chat-title">{{ chat.title || '新对话' }}</div>
              <div class="chat-preview">{{ chat.lastMessage || '开始新的对话...' }}</div>
              <div class="chat-time">{{ formatTime(chat.lastTime) }}</div>
            </div>
            <button 
              class="delete-chat-btn" 
              @click.stop="deleteChat(chat.id)"
              title="删除对话"
            >
              🗑️
            </button>
          </div>
        </div>
      </div>

      <!-- 中间：当前会话聊天 -->
      <div class="chat-main">
        <div class="chat-header">
          <div class="current-chat-info">
            <h3>{{ currentChatTitle || '新对话' }}</h3>
            <span class="chat-status">{{ currentChatStatus }}</span>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-content">
              <div class="welcome-icon">🤖</div>
              <h2>欢迎使用 AI 编程小助手</h2>
              <p>我可以帮助您：</p>
              <ul>
                <li>解答编程技术问题</li>
                <li>提供代码示例和解释</li>
                <li>协助求职面试准备</li>
                <li>分享编程学习建议</li>
              </ul>
              <p>请随时向我提问吧！</p>
            </div>
          </div>

          <!-- 历史消息 -->
          <ChatMessage
            v-for="message in messages"
            :key="message.id"
            :message="message.content"
            :is-user="message.isUser"
            :timestamp="message.timestamp"
          />

          <!-- AI 正在回复的消息 -->
          <div v-if="isAiTyping" class="chat-message ai-message">
            <div class="message-avatar">
              <div class="avatar ai-avatar">AI</div>
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div class="ai-typing-content">
                  <div class="ai-response-text message-markdown" v-html="currentAiResponseRendered"></div>
                  <LoadingDots v-if="isStreaming" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入框 -->
        <ChatInput
          :disabled="isAiTyping"
          @send-message="sendMessage"
          placeholder="请输入您的编程问题..."
        />
      </div>

      <!-- 右侧：用户登录信息 -->
      <div class="sidebar-right">
        <div class="user-profile">
          <div class="profile-header">
            <h3>用户信息</h3>
          </div>
          
          <div class="profile-content">
            <div class="avatar-section">
              <div class="user-avatar">
                <span class="avatar-text">{{ userInitials }}</span>
              </div>
              <div class="user-info">
                <div class="username">{{ userName || '未登录' }}</div>
                <div class="user-email">{{ userEmail || '点击登录' }}</div>
              </div>
            </div>
            
            <div class="login-section" v-if="!isLoggedIn">
              <button class="login-btn" @click="showLoginModal = true">
                登录/注册
              </button>
            </div>
            
            <div class="user-stats" v-if="isLoggedIn">
              <div class="stat-item">
                <div class="stat-label">对话总数</div>
                <div class="stat-value">{{ totalChats }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">消息总数</div>
                <div class="stat-value">{{ totalMessages }}</div>
              </div>
            </div>
            
            <div class="user-actions" v-if="isLoggedIn">
              <button class="action-btn" @click="showSettings = true">
                ⚙️ 设置
              </button>
              <button class="action-btn logout-btn" @click="logout">
                🚪 退出登录
              </button>
            </div>
          </div>
        </div>

        <!-- 快速操作 -->
        <div class="quick-actions">
          <div class="section-header">
            <h4>快速操作</h4>
          </div>
          <div class="action-grid">
            <button class="quick-action-btn" @click="exportChat">
              📤 导出对话
            </button>
            <button class="quick-action-btn" @click="clearHistory">
              🗑️ 清空历史
            </button>
            <button class="quick-action-btn" @click="showHelp = true">
              ❓ 帮助
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 登录模态框 -->
    <div v-if="showLoginModal" class="modal-overlay" @click="showLoginModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>用户登录</h3>
          <button class="close-btn" @click="showLoginModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="loginForm.username" type="text" placeholder="请输入用户名">
          </div>
          <div class="form-group">
            <label>密码</label>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码">
          </div>
          <div class="form-actions">
            <button class="btn-primary" @click="handleLogin">登录</button>
            <button class="btn-secondary" @click="handleRegister">注册</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 连接状态提示 -->
    <div v-if="connectionError" class="connection-error">
      <div class="error-content">
        <span class="error-icon">⚠️</span>
        <span>连接服务器失败，请检查后端服务是否启动</span>
      </div>
    </div>
  </div>
</template>

<script>
import ChatMessage from './components/ChatMessage.vue'
import ChatInput from './components/ChatInput.vue'
import LoadingDots from './components/LoadingDots.vue'
import { chatWithSSE } from './api/chatApi.js'
import { generateMemoryId } from './utils/index.js'
import { marked } from 'marked'

export default {
  name: 'App',
  components: {
    ChatMessage,
    ChatInput,
    LoadingDots
  },
  data() {
    return {
      // 聊天相关
      messages: [],
      currentChatId: null,
      chatHistory: [],
      memoryId: null,
      isAiTyping: false,
      isStreaming: false,
      currentAiResponse: '',
      currentEventSource: null,
      connectionError: false,
      
      // 用户相关
      isLoggedIn: false,
      userName: '',
      userEmail: '',
      showLoginModal: false,
      showSettings: false,
      showHelp: false,
      
      // 登录表单
      loginForm: {
        username: '',
        password: ''
      }
    }
  },
  computed: {
    currentAiResponseRendered() {
      if (!this.currentAiResponse) return ''
      marked.setOptions({
        breaks: true,
        gfm: true,
        sanitize: false,
        highlight: function(code, lang) {
          return code
        }
      })
      return marked(this.currentAiResponse)
    },
    
    currentChatTitle() {
      const chat = this.chatHistory.find(c => c.id === this.currentChatId)
      return chat ? chat.title : '新对话'
    },
    
    currentChatStatus() {
      if (this.isAiTyping) return 'AI 正在思考...'
      if (this.messages.length === 0) return '开始新的对话'
      return `${this.messages.length} 条消息`
    },
    
    userInitials() {
      if (!this.userName) return '?'
      return this.userName.substring(0, 2).toUpperCase()
    },
    
    totalChats() {
      return this.chatHistory.length
    },
    
    totalMessages() {
      return this.chatHistory.reduce((total, chat) => total + (chat.messageCount || 0), 0)
    }
  },
  methods: {
    // 聊天相关方法
    sendMessage(message) {
      this.addMessage(message, true)
      this.startAiResponse(message)
      this.updateChatHistory()
    },
    
    addMessage(content, isUser = false) {
      const message = {
        id: Date.now() + Math.random(),
        content,
        isUser,
        timestamp: new Date()
      }
      this.messages.push(message)
      this.scrollToBottom()
    },
    
    startAiResponse(userMessage) {
      this.isAiTyping = true
      this.isStreaming = true
      this.currentAiResponse = ''
      this.connectionError = false
      
      if (this.currentEventSource) {
        this.currentEventSource.close()
      }
      
      this.currentEventSource = chatWithSSE(
        this.memoryId,
        userMessage,
        this.handleAiMessage,
        this.handleAiError,
        this.handleAiClose
      )
    },
    
    handleAiMessage(data) {
      this.currentAiResponse += data
      this.scrollToBottom()
    },
    
    handleAiError(error) {
      console.error('AI 回复出错:', error)
      this.connectionError = true
      this.finishAiResponse()
      
      setTimeout(() => {
        this.connectionError = false
      }, 5000)
    },
    
    handleAiClose() {
      this.finishAiResponse()
    },
    
    finishAiResponse() {
      this.isStreaming = false
      
      if (this.currentAiResponse.trim()) {
        this.addMessage(this.currentAiResponse.trim(), false)
        this.updateChatHistory()
      }
      
      this.isAiTyping = false
      this.currentAiResponse = ''
      this.connectionError = false
      
      if (this.currentEventSource) {
        this.currentEventSource.close()
        this.currentEventSource = null
      }
    },
    
    // 聊天历史管理
    startNewChat() {
      const newChatId = generateMemoryId()
      const newChat = {
        id: newChatId,
        title: '新对话',
        lastMessage: '开始新的对话...',
        lastTime: new Date(),
        messageCount: 0
      }
      
      this.chatHistory.unshift(newChat)
      this.switchToChat(newChatId)
      this.memoryId = newChatId
      this.messages = []
    },
    
    switchToChat(chatId) {
      this.currentChatId = chatId
      this.memoryId = chatId
      
      // 这里应该从存储中加载对应的消息
      // 暂时使用模拟数据
      this.messages = []
    },
    
    deleteChat(chatId) {
      const index = this.chatHistory.findIndex(c => c.id === chatId)
      if (index > -1) {
        this.chatHistory.splice(index, 1)
        
        if (this.currentChatId === chatId) {
          if (this.chatHistory.length > 0) {
            this.switchToChat(this.chatHistory[0].id)
          } else {
            this.startNewChat()
          }
        }
      }
    },
    
    updateChatHistory() {
      if (this.currentChatId && this.messages.length > 0) {
        const chat = this.chatHistory.find(c => c.id === this.currentChatId)
        if (chat) {
          const lastMessage = this.messages[this.messages.length - 1]
          chat.lastMessage = lastMessage.content.substring(0, 50) + (lastMessage.content.length > 50 ? '...' : '')
          chat.lastTime = lastMessage.timestamp
          chat.messageCount = this.messages.length
          
          // 更新标题（使用第一条用户消息作为标题）
          const firstUserMessage = this.messages.find(m => m.isUser)
          if (firstUserMessage && !chat.title.startsWith('新对话')) {
            chat.title = firstUserMessage.content.substring(0, 30) + (firstUserMessage.content.length > 30 ? '...' : '')
          }
        }
      }
    },
    
    // 用户相关方法
    handleLogin() {
      if (this.loginForm.username && this.loginForm.password) {
        this.isLoggedIn = true
        this.userName = this.loginForm.username
        this.userEmail = `${this.loginForm.username}@example.com`
        this.showLoginModal = false
        this.loginForm = { username: '', password: '' }
      }
    },
    
    handleRegister() {
      if (this.loginForm.username && this.loginForm.password) {
        this.isLoggedIn = true
        this.userName = this.loginForm.username
        this.userEmail = `${this.loginForm.username}@example.com`
        this.showLoginModal = false
        this.loginForm = { username: '', password: '' }
      }
    },
    
    logout() {
      this.isLoggedIn = false
      this.userName = ''
      this.userEmail = ''
    },
    
    // 工具方法
    formatTime(date) {
      if (!date) return ''
      const now = new Date()
      const diff = now - new Date(date)
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 7) return `${days}天前`
      
      return new Date(date).toLocaleDateString()
    },
    
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    initializeChat() {
      this.memoryId = generateMemoryId()
      this.startNewChat()
      console.log('聊天室ID:', this.memoryId)
    },
    
    // 快速操作方法
    exportChat() {
      if (this.messages.length === 0) return
      
      const chatContent = this.messages.map(msg => 
        `${msg.isUser ? '用户' : 'AI'}: ${msg.content}`
      ).join('\n\n')
      
      const blob = new Blob([chatContent], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `chat-${this.currentChatId}.txt`
      a.click()
      URL.revokeObjectURL(url)
    },
    
    clearHistory() {
      if (confirm('确定要清空所有聊天记录吗？此操作不可恢复。')) {
        this.chatHistory = []
        this.messages = []
        this.startNewChat()
      }
    }
  },
  
  mounted() {
    this.initializeChat()
  },
  
  beforeUnmount() {
    if (this.currentEventSource) {
      this.currentEventSource.close()
    }
  }
}
</script>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
}

.app-header {
  background-color: #fff;
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.app-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.app-subtitle {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.main-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧边栏 */
.sidebar-left {
  width: 280px;
  background-color: #fff;
  border-right: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
}

.sidebar-header h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.new-chat-btn {
  width: 100%;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  transition: background-color 0.2s;
}

.new-chat-btn:hover {
  background-color: #0056b3;
}

.new-chat-btn .icon {
  font-size: 16px;
  font-weight: bold;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.chat-item:hover {
  background-color: #f8f9fa;
}

.chat-item.active {
  background-color: #e3f2fd;
  border-left: 3px solid #2196f3;
}

.chat-item-content {
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-preview {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-time {
  font-size: 11px;
  color: #999;
}

.delete-chat-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.chat-item:hover .delete-chat-btn {
  opacity: 1;
}

.delete-chat-btn:hover {
  background-color: #ffebee;
}

/* 中间聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
  background-color: #fafbfc;
}

.current-chat-info h3 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 18px;
}

.chat-status {
  font-size: 12px;
  color: #666;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.welcome-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.welcome-content {
  text-align: center;
  max-width: 400px;
  color: #666;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.welcome-content h2 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #333;
}

.welcome-content p {
  margin-bottom: 10px;
  line-height: 1.5;
}

.welcome-content ul {
  text-align: left;
  margin: 15px 0;
}

.welcome-content li {
  margin-bottom: 5px;
}

/* AI 正在回复时的消息样式 */
.chat-message {
  display: flex;
  margin-bottom: 20px;
  padding: 0 20px;
}

.ai-message {
  justify-content: flex-start;
  flex-direction: row;
}

.message-avatar {
  display: flex;
  align-items: flex-start;
  margin: 0 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: white;
}

.ai-avatar {
  background-color: #6c757d;
}

.message-content {
  max-width: 70%;
  min-width: 100px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  word-break: break-word;
  background-color: #f1f3f4;
  color: #333;
  border-bottom-left-radius: 4px;
}

.ai-typing-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-response-text {
  font-size: 14px;
  line-height: 1.5;
}

/* AI实时回复的Markdown样式 */
.ai-response-text.message-markdown h1,
.ai-response-text.message-markdown h2,
.ai-response-text.message-markdown h3,
.ai-response-text.message-markdown h4,
.ai-response-text.message-markdown h5,
.ai-response-text.message-markdown h6 {
  margin: 0.5em 0;
  font-weight: bold;
}

.ai-response-text.message-markdown h1 { font-size: 1.5em; }
.ai-response-text.message-markdown h2 { font-size: 1.3em; }
.ai-response-text.message-markdown h3 { font-size: 1.2em; }
.ai-response-text.message-markdown h4 { font-size: 1.1em; }
.ai-response-text.message-markdown h5 { font-size: 1em; }
.ai-response-text.message-markdown h6 { font-size: 0.9em; }

.ai-response-text.message-markdown p {
  margin: 0.5em 0;
}

.ai-response-text.message-markdown ul,
.ai-response-text.message-markdown ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.ai-response-text.message-markdown li {
  margin: 0.2em 0;
}

.ai-response-text.message-markdown code {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

.ai-response-text.message-markdown pre {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 1em;
  border-radius: 5px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.ai-response-text.message-markdown pre code {
  background-color: transparent;
  padding: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

.ai-response-text.message-markdown blockquote {
  border-left: 4px solid #ccc;
  padding-left: 1em;
  margin: 0.5em 0;
  font-style: italic;
  color: #666;
}

.ai-response-text.message-markdown a {
  color: #007bff;
  text-decoration: underline;
}

.ai-response-text.message-markdown table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
}

.ai-response-text.message-markdown th,
.ai-response-text.message-markdown td {
  border: 1px solid #ddd;
  padding: 0.5em;
  text-align: left;
}

.ai-response-text.message-markdown th {
  background-color: #f2f2f2;
  font-weight: bold;
}

.ai-response-text.message-markdown hr {
  border: none;
  border-top: 1px solid #ddd;
  margin: 1em 0;
}

/* 右侧边栏 */
.sidebar-right {
  width: 280px;
  background-color: #fff;
  border-left: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
}

.user-profile {
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
}

.profile-header h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 16px;
}

.avatar-section {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #007bff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.avatar-text {
  color: white;
  font-size: 20px;
  font-weight: bold;
}

.user-info .username {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-info .user-email {
  font-size: 12px;
  color: #666;
}

.login-section {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  padding: 10px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.login-btn:hover {
  background-color: #218838;
}

.user-stats {
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.stat-value {
  font-weight: 500;
  color: #333;
}

.user-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 继续补充用户操作按钮样式 */
.user-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  padding: 8px 12px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-btn:hover {
  background-color: #e9ecef;
}

.logout-btn {
  background-color: #fff5f5;
  border-color: #ffe3e3;
  color: #e74c3c;
}

.logout-btn:hover {
  background-color: #ffe3e3;
}

/* 快速操作区域样式 */
.quick-actions {
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
}

.section-header h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 14px;
}

.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-action-btn {
  padding: 10px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.quick-action-btn:hover {
  background-color: #e9ecef;
}

/* 模态框样式完善 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 10px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: #f1f1f1;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary {
  flex: 1;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  flex: 1;
  padding: 10px;
  background-color: #f8f9fa;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background-color: #e9ecef;
}

/* 连接错误提示样式 */
.connection-error {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #fff5f5;
  border: 1px solid #ffe3e3;
  border-radius: 8px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translate(-50%, 20px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}

.error-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-icon {
  color: #e74c3c;
  font-size: 18px;
}

/* 聊天输入框样式 */
.chat-input-container {
  padding: 15px 20px;
  border-top: 1px solid #e9ecef;
  background-color: #fafbfc;
}

.input-wrapper {
  display: flex;
  gap: 10px;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 14px;
  resize: none;
  min-height: 48px;
  max-height: 120px;
  overflow-y: auto;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.message-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.send-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s, transform 0.1s;
}

.send-btn:hover {
  background-color: #0056b3;
}

.send-btn:active {
  transform: scale(0.95);
}

.send-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 聊天消息组件样式补充 */
.message-meta {
  font-size: 11px;
  color: #999;
  margin-top: 5px;
  display: flex;
  justify-content: flex-end;
}

.user-message {
  justify-content: flex-end;
}

.user-message .message-bubble {
  background-color: #007bff;
  color: white;
  border-bottom-right-radius: 4px;
  border-bottom-left-radius: 18px;
}

.user-message .message-avatar {
  order: 1;
}

.user-avatar {
  background-color: #28a745;
}

/* 加载动画样式 */
.loading-dots {
  display: flex;
  gap: 4px;
  align-items: center;
  justify-content: center;
  height: 20px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #666;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots .loading-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots .loading-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 响应式调整 */
@media (max-width: 992px) {
  .sidebar-right {
    display: none;
  }
  
  .sidebar-left {
    width: 240px;
  }
}

@media (max-width: 768px) {
  .sidebar-left {
    display: none;
  }
  
  .app-title {
    font-size: 20px;
  }
  
  .message-content {
    max-width: 85%;
  }
}

</style>