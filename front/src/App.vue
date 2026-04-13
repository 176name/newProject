<template>
  <router-view />
</template>

<style>
/* 强制背景色，用来判断组件是否加载 */
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100vw;
  height: 100vh;
  background-color: #112244 !important;
}
</style>
