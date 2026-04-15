<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
onLaunch(() => {
  // #ifdef MP-WEIXIN
  const _wx = globalThis.wx
  if (_wx && _wx.cloud) {
    _wx.cloud.init({
      env: "prod-3g0nnk2k58e9785d"
    })
  }
  // #endif
  console.log("App Launch");
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>
<style></style>
