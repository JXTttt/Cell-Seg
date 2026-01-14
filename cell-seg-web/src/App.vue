<template>
  <div class="common-layout">
    <el-container class="root-container">
      
      <el-aside width="200px" class="left-aside">
        <div class="logo-area">
          <el-icon :size="24" color="#409EFF"><Monitor /></el-icon>
          <span class="system-title">尿液检测系统</span>
        </div>

        <el-menu
          default-active="1"
          class="el-menu-vertical"
          background-color="#fff"
          text-color="#303133"
          active-text-color="#409EFF"
        >
          <input type="file" ref="fileInput" @change="handleFileChange" style="display: none" />

          <el-menu-item index="1" @click="$refs.fileInput.click()">
            <el-icon><FolderOpened /></el-icon>
            <span>选择文件</span>
          </el-menu-item>

          <el-menu-item index="2">
            <el-icon><MagicStick /></el-icon>
            <span>图像增广 (预留)</span>
          </el-menu-item>

          <el-menu-item index="3" @click="uploadImage" :disabled="!selectedFile || loading">
            <el-icon><VideoPlay /></el-icon>
            <span>开始检测</span>
          </el-menu-item>

          <el-divider />

          <el-menu-item index="4" @click="currentView = 'original'" :disabled="!imageUrl">
            <el-icon><Picture /></el-icon>
            <span>查看原图</span>
          </el-menu-item>

          <el-menu-item index="5" @click="currentView = 'result'" :disabled="!resultImageUrl">
            <el-icon><PictureFilled /></el-icon>
            <span>查看结果图</span>
          </el-menu-item>

          <el-menu-item index="6">
            <el-icon><Download /></el-icon>
            <span>保存结果</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container class="right-big-container">
        
        <el-header class="app-header">
          <div class="header-left">
            <span v-if="selectedFile" style="font-size: 12px; color: #909399">
              当前文件: {{ selectedFile.name }}
            </span>
          </div>
          <div class="header-right">
            <el-icon :size="20" style="margin-right: 20px; cursor: pointer"><Bell /></el-icon>
            <el-avatar :size="30" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
            <span class="username">测试用户</span>
          </div>
        </el-header>

        <el-container class="core-content">
          
          <el-main class="main-display">
            <div class="image-stage" v-loading="loading" element-loading-text="AI 正在识别中...">
              
              <img 
                v-if="displayImage" 
                :src="displayImage" 
                class="responsive-img" 
                :style="imageFilterStyle"
              />
              
              <div v-else class="empty-state">
                <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" style="width: 120px; opacity: 0.6;" />
                <p style="color: #8c8c8c; margin-top: 10px;">请先在左侧选择图片</p>
              </div>

              <div class="view-tag" v-if="displayImage">
                {{ currentView === 'original' ? '原图模式' : '识别结果' }}
              </div>
            </div>

            <div class="footer-status">
              <span>当前模型: YOLO11s-seg (Best)</span>
              <span>系统状态: {{ loading ? '运行中...' : '就绪' }}</span>
            </div>
          </el-main>

          <el-aside width="280px" class="right-aside">
            
            <div class="panel-card">
              <div class="card-title">检测结果</div>
              <div class="result-list">
                <div class="result-item">
                  <span class="label">检测耗时：</span>
                  <span class="value">{{ detectTime }}s</span>
                </div>
                <div class="result-item">
                  <span class="label">总计数目：</span>
                  <span class="value">{{ totalCells }}</span>
                </div>
                <el-divider style="margin: 12px 0" />
                
                <div class="cell-stat" v-for="(count, name) in formattedStats" :key="name">
                  <span class="dot" :style="{background: getColor(name)}"></span>
                  <span class="label">{{ getCNName(name) }}：</span>
                  <span class="value">{{ count }}</span>
                </div>
              </div>
            </div>

            <div class="panel-card">
              <div class="card-title">图像调整</div>
              <div class="slider-group">
                <span class="slider-label">亮度 (Brightness)</span>
                <el-slider v-model="imgSettings.brightness" :min="50" :max="150" size="small" />
              </div>
              <div class="slider-group">
                <span class="slider-label">对比度 (Contrast)</span>
                <el-slider v-model="imgSettings.contrast" :min="50" :max="150" size="small" />
              </div>
              <div class="slider-group">
                <span class="slider-label">饱和度 (Saturate)</span>
                <el-slider v-model="imgSettings.saturate" :min="0" :max="200" size="small" />
              </div>
              <div style="text-align: right; margin-top: 10px;">
                 <el-button size="small" @click="resetSettings">重置</el-button>
              </div>
            </div>

          </el-aside>
        </el-container>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { 
  Monitor, FolderOpened, MagicStick, VideoPlay, 
  Picture, PictureFilled, Download, Bell 
} from '@element-plus/icons-vue'

// --- 状态变量 ---
const fileInput = ref(null)
const selectedFile = ref(null)
const loading = ref(false)
const imageUrl = ref('')         
const resultImageUrl = ref('')   
const currentView = ref('original') 
const summaryData = ref(null)
const detectTime = ref(0) 
const imgSettings = ref({ brightness: 100, contrast: 100, saturate: 100 })

// --- 计算属性 ---
const displayImage = computed(() => {
  if (currentView.value === 'original') return imageUrl.value
  if (currentView.value === 'result') return resultImageUrl.value
  return ''
})

const imageFilterStyle = computed(() => {
  return {
    filter: `brightness(${imgSettings.value.brightness}%) contrast(${imgSettings.value.contrast}%) saturate(${imgSettings.value.saturate}%)`
  }
})

const totalCells = computed(() => {
  if (!summaryData.value) return 0
  return Object.values(summaryData.value).reduce((a, b) => a + b, 0)
})

const formattedStats = computed(() => {
  return summaryData.value || { 'rbc': 0, 'wbc': 0, 'crystal': 0 }
})

// --- 辅助函数 ---
const getCNName = (key) => {
  const map = { 'rbc': '红细胞', 'wbc': '白细胞', 'crystal': '结晶' }
  return map[key] || key
}
const getColor = (key) => {
  const map = { 'rbc': 'red', 'wbc': 'orange', 'crystal': 'green' }
  return map[key] || '#ccc'
}
const resetSettings = () => {
  imgSettings.value = { brightness: 100, contrast: 100, saturate: 100 }
}

// --- 业务逻辑 ---
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    selectedFile.value = file
    imageUrl.value = URL.createObjectURL(file)
    resultImageUrl.value = '' 
    currentView.value = 'original' 
    summaryData.value = null
  }
}

const uploadImage = async () => {
  if (!selectedFile.value) return
  loading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('userId', 101) 
  const startTime = Date.now()

  try {
    const res = await axios.post('http://localhost:8080/api/analysis/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200) {
      detectTime.value = ((Date.now() - startTime) / 1000).toFixed(3)
      const result = res.data.data
      ElMessage.success('识别完成')
      summaryData.value = JSON.parse(result.summaryJson)
      
      const rawPath = result.imageUrl
      const fileName = rawPath.split('\\').pop()
      imageUrl.value = `http://localhost:8080/images/${fileName}`

      if (result.resultImageUrl) {
        const resFileName = result.resultImageUrl.split('\\').pop()
        resultImageUrl.value = `http://localhost:8080/images/${resFileName}`
        currentView.value = 'result'
      }
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<style>
/* 🔴 核心修复：强制所有元素使用边框盒模型，防止 padding 撑大元素 */
*, *::before, *::after {
  box-sizing: border-box;
}

html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden; /* 禁止页面级滚动，消灭任何可能的白边 */
  background-color: #f0f2f5;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
</style>

<style scoped>
/* 根容器 */
.common-layout {
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.root-container {
  width: 100%;
  height: 100%;
}

/* 左侧侧边栏 */
.left-aside {
  background-color: white;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  flex-shrink: 0; /* 禁止被压缩 */
  z-index: 10; /* 确保阴影或边框层级正确 */
}

.logo-area {
  height: 55px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.system-title {
  font-weight: bold;
  font-size: 15px;
  color: #303133;
}

.el-menu-vertical {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

/* 右侧大容器 (包含 Header + Content) */
.right-big-container {
  flex: 1; /* 🔴 关键：自动占据剩余所有宽度 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0; /* 防止子元素过大撑破布局 */
  background-color: #f0f2f5;
}

/* 顶部 Header - 🔴 修复重点 */
.app-header {
  background-color: white;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 55px !important;
  flex-shrink: 0;
  /* 🔴 删除 width: 100%; 让 Flexbox 自动撑满，防止计算误差 */
  /* width: 100%;  <-- 已删除 */
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  user-select: none;
}

/* 核心内容区 */
.core-content {
  flex: 1; /* 占据剩余垂直空间 */
  display: flex;
  overflow: hidden;
  padding: 12px; /* 稍微增加一点间隙，更好看 */
  gap: 12px; /* 左右两栏的间距 */
}

/* 中间大屏 */
.main-display {
  flex: 1; /* 🔴 撑满剩余水平空间 */
  background-color: white;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  min-width: 0; /* 防止图片过大撑破 */
}

/* 深色舞台 */
.image-stage {
  flex: 1;
  width: 100%;
  background-color: #2c3e50;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #8c939d;
}

.responsive-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: filter 0.3s;
}

.view-tag {
  position: absolute;
  top: 15px;
  left: 15px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.footer-status {
  height: 32px;
  background: #fdfdfd;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

/* 右侧侧边栏 */
.right-aside {
  /* 宽度由 HTML 标签 width 属性控制 (建议280px) */
  background: transparent;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  flex-shrink: 0; 
}

.panel-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.card-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 12px;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
  color: #303133;
}

.result-item, .cell-stat {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  display: inline-block;
}

.slider-group {
  margin-bottom: 12px;
}

.slider-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  display: block;
}
</style>