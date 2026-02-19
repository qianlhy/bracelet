<template>
  <view class="page">
    <!-- 品牌头部 -->
    <view class="brand-header">
      <view class="logo">🔮</view>
      <view class="brand-name">念初手作</view>
      <view class="brand-slogan">天然水晶 · 念初定制</view>
      <view class="version">v1.0.0</view>
    </view>

    <!-- 关于我们 -->
    <view class="section">
      <view class="section-title">关于我们</view>
      <view class="section-content">
        <text class="content-text">
          我们是一家专注于水晶手链定制的品牌，致力于为每一位用户打造独一无二的手链作品。
          从选材到设计，从制作到包装，我们坚持用心精神，只为呈现最美的作品。
        </text>
      </view>
    </view>

    <!-- 品牌理念 -->
    <view class="section">
      <view class="section-title">品牌理念</view>
      <view class="feature-list">
        <view class="feature-item">
          <view class="feature-icon">💎</view>
          <view class="feature-info">
            <view class="feature-name">精选原石</view>
            <view class="feature-desc">每一颗珠子都经过精心挑选</view>
          </view>
        </view>
        <view class="feature-item">
          <view class="feature-icon">✨</view>
          <view class="feature-info">
            <view class="feature-name">用心工艺</view>
            <view class="feature-desc">传统工艺与现代设计完美结合</view>
          </view>
        </view>
        <view class="feature-item">
          <view class="feature-icon">🎨</view>
          <view class="feature-info">
            <view class="feature-name">自由定制</view>
            <view class="feature-desc">DIY创作，专属你的独特风格</view>
          </view>
        </view>
        <view class="feature-item">
          <view class="feature-icon">🌟</view>
          <view class="feature-info">
            <view class="feature-name">品质保证</view>
            <view class="feature-desc">严格质检，确保每件作品完美</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 联系我们 -->
    <!-- <view class="section">
      <view class="section-title">联系我们</view>
      <view class="contact-list">
        <view class="contact-item">
          <view class="contact-label">客服微信</view>
          <view class="contact-value">点击查看二维码</view>
          <button class="contact-btn" @click="showQRCode">查看</button>
        </view>
        <view class="contact-item">
          <view class="contact-label">营业时间</view>
          <view class="contact-value">周一至周日 9:00-21:00</view>
        </view> -->
        <!-- <view class="contact-item">
          <view class="contact-label">客服电话</view>
          <view class="contact-value">400-123-4567</view>
          <button class="contact-btn" @click="callPhone">拨打</button>
        </view> -->
      <!-- </view>
    </view> -->

    <!-- 版权信息 -->
    <view class="footer">
      <view class="copyright">© 2025 念初手作</view>
      <view class="copyright">专注水晶手链定制</view>
    </view>

    <!-- 客服二维码弹窗 -->
    <view v-if="showQR" class="mask" @click="showQR = false">
      <view class="qr-modal" @click.stop>
        <view class="qr-title">客服微信</view>
        <image 
          class="qr" 
          :src="qrUrl" 
          mode="widthFix" 
          show-menu-by-longpress="true"
          @click="previewQR"
        />
        <view class="qr-tips">长按识别二维码添加客服</view>
        <button class="qr-close" @click="showQR = false">关闭</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const showQR = ref(false)
const qrUrl = '/static/CustomerService/714966e4f87775b79a26b9002c0606d1.jpg'

function showQRCode() {
  showQR.value = true
}

function previewQR() {
  try {
    uni.previewImage({ urls: [qrUrl] })
  } catch (e) {
    uni.showToast({ title: '图片加载失败', icon: 'none' })
  }
}

function callPhone() {
  uni.makePhoneCall({
    phoneNumber: '400-123-4567',
    fail: () => {
      uni.showToast({ title: '拨打失败', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f7f7;
  padding-bottom: 24rpx;
}

.brand-header {
  background: linear-gradient(135deg, #ffe88f, #ffd84c);
  padding: 80rpx 24rpx 60rpx;
  text-align: center;
}

.logo {
  font-size: 120rpx;
  margin-bottom: 24rpx;
}

.brand-name {
  font-size: 48rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 12rpx;
}

.brand-slogan {
  font-size: 28rpx;
  color: #5f4500;
  margin-bottom: 16rpx;
}

.version {
  font-size: 24rpx;
  color: #999;
}

.section {
  background: #fff;
  margin: 24rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 32rpx 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid #f5f5f5;
}

.section-content {
  line-height: 1.8;
}

.content-text {
  font-size: 28rpx;
  color: #666;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.feature-icon {
  width: 80rpx;
  height: 80rpx;
  font-size: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.feature-info {
  flex: 1;
}

.feature-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 6rpx;
}

.feature-desc {
  font-size: 24rpx;
  color: #999;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.contact-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.contact-item:last-child {
  border-bottom: none;
}

.contact-label {
  font-size: 28rpx;
  color: #666;
  width: 160rpx;
  flex-shrink: 0;
}

.contact-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.contact-btn {
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
  background: #ffd84c;
  color: #333;
  border: none;
  border-radius: 8rpx;
  font-weight: 600;
}

.footer {
  text-align: center;
  padding: 48rpx 24rpx;
}

.copyright {
  font-size: 24rpx;
  color: #999;
  line-height: 1.8;
}

.mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.qr-modal {
  width: 80%;
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 24rpx;
}

.qr {
  width: 480rpx;
  height: 480rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}

.qr-tips {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 24rpx;
}

.qr-close {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #ffd84c;
  color: #333;
  font-size: 28rpx;
  font-weight: 700;
  border-radius: 16rpx;
  border: none;
}
</style>
