<template>
  <div class="app-shell">
    <!-- 导航栏 -->
    <nav class="navbar">
      <router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }">
        股票
      </router-link>
      <router-link to="/map" class="nav-link" :class="{ active: $route.path === '/map' }">
        地图
      </router-link>
    </nav>

    <!-- 路由页面 -->
    <router-view class="router-content" />
  </div>
</template>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }

html,
body,
#app {
  height: 100%;
}

body {
  background: #0a0e1a;
  color: #e0e6ed;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
}

.app-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #0a0e1a;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  gap: 4px;
  background: #0d1425;
  border-bottom: 1px solid #1a2540;
  padding: 0 16px;
}

.nav-link {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #4a5a7a;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}

.nav-link:hover {
  color: #e0e6ed;
}

.nav-link.active {
  color: #3c78d8;
  border-bottom-color: #3c78d8;
}

.router-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
