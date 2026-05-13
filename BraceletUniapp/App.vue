<template>
  <view />
</template>

<script>
import { checkAndAutoLogin } from './api/index.js'
import { updateCartBadgeNow } from './utils/cartBadge.js'

export default {
  onLaunch() {
    // 应用启动时立即更新购物车角标
    setTimeout(() => {
      updateCartBadgeNow()
    }, 500)
    
    // 应用启动时尝试自动登录
    checkAndAutoLogin().then((success) => {
      if (success) {
        console.log('[App] 自动登录成功')
      }
    })
  },
  onShow() {
    // 应用从后台进入前台时更新购物车角标
    updateCartBadgeNow()
    
    // 应用从后台进入前台时尝试自动登录
    checkAndAutoLogin().then((success) => {
      if (success) {
        console.log('[App] 自动登录成功')
      }
    })
  },
  onHide() {}
}
</script>

<style>
page { background-color: #f6f7fb; }
</style>
