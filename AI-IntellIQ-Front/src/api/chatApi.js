import axios from 'axios'

// 配置axios基础URL
const API_BASE_URL = 'http://localhost:8081/api'

/**
 * 使用 SSE 方式调用聊天接口
 * @param {number} memoryId 聊天室ID
 * @param {string} message 用户消息
 * @param {Function} onMessage 接收消息的回调函数
 * @param {Function} onError 错误处理回调函数
 * @param {Function} onClose 连接关闭回调函数
 * @returns {EventSource} 返回 EventSource 对象，用于手动关闭连接
 */
export function chatWithSSE(memoryId, message, onMessage, onError, onClose) {
    // 构建URL参数
    const params = new URLSearchParams({
        memoryId: memoryId,
        message: message
    })
    
    // 创建 EventSource 连接
    const eventSource = new EventSource(`${API_BASE_URL}/ai/chat?${params}`)
    
    // 处理接收到的消息
    eventSource.onmessage = function(event) {
        try {
            const data = event.data
            if (data && data.trim() !== '') {
                onMessage(data)
            }
        } catch (error) {
            console.error('解析消息失败:', error)
            onError && onError(error)
        }
    }
    
    // 处理错误
    eventSource.onerror = function(error) {
        console.log('SSE 连接状态:', eventSource.readyState)
        // 只有在连接状态不是正常关闭时才报错
        if (eventSource.readyState !== EventSource.CLOSED) {
            console.error('SSE 连接错误:', error)
            onError && onError(error)
        } else {
            console.log('SSE 连接正常结束')
        }
        
        // 确保连接关闭
        if (eventSource.readyState !== EventSource.CLOSED) {
            eventSource.close()
        }
    }
    
    // 处理连接关闭
    eventSource.onclose = function() {
        console.log('SSE 连接已关闭')
        onClose && onClose()
    }
    
    return eventSource
}

/**
 * 检查后端服务是否可用
 * @returns {Promise<boolean>} 返回服务是否可用
 */
export async function checkServiceHealth() {
    try {
        const response = await axios.get(`${API_BASE_URL}/health`, {
            timeout: 5000
        })
        return response.status === 200
    } catch (error) {
        console.error('服务健康检查失败:', error)
        return false
    }
} 