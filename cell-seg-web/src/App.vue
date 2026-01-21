<template>
  <div class="common-layout">
    <el-container class="root-container">
      
      <el-aside width="160px" class="left-aside">
        <div class="logo-area">
          <el-icon :size="24" color="#409EFF"><Monitor /></el-icon>
          <span class="system-title">尿液检测</span>
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
            <span>图像增广</span>
          </el-menu-item>

          <el-menu-item index="3" @click="uploadImage" :disabled="!selectedFile || loading">
            <el-icon><VideoPlay /></el-icon>
            <span>开始检测</span>
          </el-menu-item>

          <el-divider />

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
              
              <div v-if="imageUrl" class="dual-view-container">
                <div class="img-wrapper">
                  <div class="img-tag original">原图 (Source)</div>
                  <img 
                    :src="imageUrl" 
                    class="responsive-img" 
                    :style="imageFilterStyle"
                  />
                </div>

                <div class="img-wrapper" v-if="resultImageUrl">
                  <div class="img-tag result">识别结果 (Result)</div>
                  <img 
                    :src="resultImageUrl" 
                    class="responsive-img" 
                    :style="imageFilterStyle"
                  />
                </div>
              </div>
              
              <div v-else class="empty-state">
                <img src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg" style="width: 120px; opacity: 0.6;" />
                <p style="color: #8c8c8c; margin-top: 10px;">请先在左侧选择图片</p>
              </div>
            </div>

            <div class="control-panel">
              <div class="control-header">
                <el-icon><MagicStick /></el-icon>
                <span>图像增强调节</span>
              </div>
              
              <div class="control-body">
                <div class="slider-item">
                  <span class="slider-label">亮度 (Brightness)</span>
                  <el-slider v-model="imgSettings.brightness" :min="50" :max="150" size="small" />
                </div>
                <div class="slider-item">
                  <span class="slider-label">对比度 (Contrast)</span>
                  <el-slider v-model="imgSettings.contrast" :min="50" :max="150" size="small" />
                </div>
                <div class="slider-item">
                  <span class="slider-label">饱和度 (Saturate)</span>
                  <el-slider v-model="imgSettings.saturate" :min="0" :max="200" size="small" />
                </div>
                <div class="reset-btn-area">
                   <el-button size="small" @click="resetSettings" type="default" plain>重置参数</el-button>
                </div>
              </div>
            </div>

            <div class="footer-status">
              <span>当前模型: YOLO11s-seg (Best)</span>
              <span>状态: {{ loading ? '运行中...' : (resultImageUrl ? '检测完成' : '等待操作') }}</span>
            </div>
          </el-main>

          <el-aside width="280px" class="right-aside">
            
            <div class="panel-card">
              <div class="card-title">检测结果统计</div>
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
  Download, Bell 
} from '@element-plus/icons-vue'

// --- 状态变量 ---
const fileInput = ref(null)
const selectedFile = ref(null)
const loading = ref(false)
const imageUrl = ref('')         
const resultImageUrl = ref('')   
const summaryData = ref(null)
const detectTime = ref(0) 
const imgSettings = ref({ brightness: 100, contrast: 100, saturate: 100 })

// --- 计算属性 ---
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
*, *::before, *::after {
  box-sizing: border-box;
}

html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden; 
  background-color: #f0f2f5;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
</style>

<style scoped>
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

.left-aside {
  background-color: white;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  flex-shrink: 0; 
  z-index: 10;
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

.right-big-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0; 
  background-color: #f0f2f5;
}

.app-header {
  background-color: white;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 55px !important;
  flex-shrink: 0;
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

.core-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 12px;
  gap: 12px;
}

/* 主显示区容器 */
.main-display {
  flex: 1;
  background-color: white;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止内容溢出 */
  position: relative;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  min-width: 0;
}

/* 图片舞台：占据剩余所有空间 */
.image-stage {
  flex: 1; 
  width: 100%;
  background-color: #2c3e50;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 10px;
}

/* 控制面板区域 */
.control-panel {
  background-color: #fff;
  border-top: 1px solid #eee;
  padding: 12px 20px;
  flex-shrink: 0; /* 防止被压缩 */
}

.control-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 10px;
}

.control-body {
  display: flex;
  align-items: flex-end; /* 底部对齐 */
  gap: 30px; /* 间距 */
}

.slider-item {
  flex: 1; /* 平分宽度 */
  min-width: 100px;
}

.slider-label {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-bottom: -2px; /* 调整标签与滑块的距离 */
}

.reset-btn-area {
  padding-bottom: 2px;
}

/* 底部状态栏 */
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

/* 双图容器 */
.dual-view-container {
  display: flex;
  width: 100%;
  height: 100%;
  gap: 10px; 
}

.img-wrapper {
  flex: 1; 
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
  background: rgba(0,0,0,0.2); 
  border-radius: 4px;
  overflow: hidden;
}

.img-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
  z-index: 5;
  font-weight: bold;
}

.img-tag.original {
  background-color: rgba(64, 158, 255, 0.8);
}

.img-tag.result {
  background-color: rgba(103, 194, 58, 0.8);
}

.responsive-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: filter 0.3s;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #8c939d;
}

.right-aside {
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
</style>