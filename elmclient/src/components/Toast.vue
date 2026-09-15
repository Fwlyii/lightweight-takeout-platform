<template>
  <transition name="toast-fade">
    <div v-if="visible" class="toast-wrapper" :class="type">
      <div class="toast-content">
        <i :class="iconClass"></i>
        <span>{{ message }}</span>
      </div>
    </div>
  </transition>
</template>

<script>
import { ref, onMounted, computed } from 'vue';

export default {
  name: 'Toast',
  props: {
    message: {
      type: String,
      required: true
    },
    duration: {
      type: Number,
      default: 2000
    },
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'error', 'info', 'warning'].indexOf(value) !== -1
    }
  },
  setup(props, { emit }) {
    const visible = ref(false);

    const iconClass = computed(() => {
      const icons = {
        success: 'fa fa-check-circle',
        error: 'fa fa-times-circle',
        info: 'fa fa-info-circle',
        warning: 'fa fa-exclamation-circle'
      };
      return icons[props.type];
    });

    onMounted(() => {
      visible.value = true;
      setTimeout(() => {
        visible.value = false;
        setTimeout(() => {
          emit('close');
        }, 300);
      }, props.duration);
    });

    return {
      visible,
      iconClass
    };
  }
};
</script>

<style scoped>
.toast-wrapper {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;
  width: min(360px, calc(100vw - 42px));
  min-height: 74px;
  padding: 14px 18px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, .92);
  border-radius: 18px;
  background: rgba(255, 255, 255, .94);
  color: var(--skin-brand-strong, #173b60);
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif;
  font-size: 16px;
  box-shadow: 0 18px 44px rgba(var(--skin-brand-strong-rgb, 38, 86, 124), 0.2), 0 2px 10px rgba(255, 255, 255, .7) inset;
  backdrop-filter: blur(18px) saturate(1.12);
  -webkit-backdrop-filter: blur(18px) saturate(1.12);
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 13px;
}

.toast-content i {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(145deg, var(--skin-brand, #32aaf2), var(--skin-brand, #1786dd));
  font-size: 18px;
  box-shadow: 0 0 0 6px var(--skin-surface, #e8f4fc), 0 5px 12px rgba(var(--skin-brand-rgb, 22, 133, 220), 0.2);
}

.toast-content span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--skin-brand-strong, #173b60);
  font-weight: 700;
}

/* 类型样式 */
.toast-wrapper.success .toast-content i {
  background: linear-gradient(145deg, var(--skin-brand, #32aaf2), var(--skin-brand, #1786dd));
}

.toast-wrapper.error .toast-content i {
  background: linear-gradient(145deg, #ee7f87, #d95263);
  box-shadow: 0 0 0 6px #fdecef, 0 5px 12px rgba(217, 82, 99, .18);
}

.toast-wrapper.warning .toast-content i {
  background: linear-gradient(145deg, #f5b35d, #e88c35);
  box-shadow: 0 0 0 6px #fff4e6, 0 5px 12px rgba(232, 140, 53, .18);
}

.toast-wrapper.info .toast-content i {
  background: linear-gradient(145deg, var(--skin-brand, #32aaf2), var(--skin-brand, #1786dd));
}

/* 动画效果 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -30%);
}

@media (max-width: 420px) {
  .toast-wrapper {
    width: calc(100vw - 32px);
    min-height: 68px;
    padding: 13px 16px;
    border-radius: 16px;
    font-size: 15px;
  }

  .toast-content { gap: 12px; }
  .toast-content i { width: 32px; height: 32px; flex-basis: 32px; font-size: 16px; box-shadow: 0 0 0 5px var(--skin-surface, #e8f4fc), 0 4px 10px rgba(var(--skin-brand-rgb, 22, 133, 220), 0.18); }
  .toast-wrapper.error .toast-content i { box-shadow: 0 0 0 5px #fdecef, 0 4px 10px rgba(217, 82, 99, .16); }
  .toast-wrapper.warning .toast-content i { box-shadow: 0 0 0 5px #fff4e6, 0 4px 10px rgba(232, 140, 53, .16); }
}
</style>
