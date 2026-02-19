<template>
  <view />
</template>

<script>
import { updateCartBadgeNow } from './utils/cartBadge.js'

export default {
  onLaunch() {
    // 应用启动时立即更新购物车角标
    setTimeout(() => {
      updateCartBadgeNow()
    }, 500)
  },
  onShow() {
    // 应用从后台进入前台时更新购物车角标
    updateCartBadgeNow()
  },
  onHide() {}
}
</script>

<style>
page { background-color: #f6f7fb; }
</style>
