<template>
	<ul class="footer">
	  <li :class="{ active: route.path === '/index', 'is-bouncing': bouncingPath === '/index' }" @click="toIndex">
		<i class="fa fa-home"></i>
		<p>首页</p>
	  </li>
	  <li :class="{ active: route.path === '/orderList', 'is-bouncing': bouncingPath === '/orderList' }" @click="toOrderList">
		<i class="fa fa-file-text-o"></i>
		<p>订单</p>
	  </li>
	  <li :class="{ active: route.path === '/myInformation', 'is-bouncing': bouncingPath === '/myInformation' }" @click="toMyInformation">
		<i class="fa fa-user-o"></i>
		<p>我的</p>
	  </li>
	</ul>
  </template>

  <script>
  import { defineComponent, onBeforeUnmount, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  export default defineComponent({
	name: 'Footer',
	setup() {
	  const router = useRouter();
	  const route = useRoute();
	  // 纯交互动效状态：记录刚点击的导航项，用于播放一次弹跳动画。
	  const bouncingPath = ref('');
	  let bounceTimer = null;
	  const ping = (path) => {
		bouncingPath.value = path;
		clearTimeout(bounceTimer);
		bounceTimer = setTimeout(() => { bouncingPath.value = ''; }, 220);
	  };
	  onBeforeUnmount(() => clearTimeout(bounceTimer));
	  const toIndex = () => {
		ping('/index');
		router.push({ path: '/index' });
	  };

	  const toOrderList = () => {
		ping('/orderList');
		router.push({ path: '/orderList' });
	  };
	  const toMyInformation = () => {
		ping('/myInformation');
		router.push({ path: '/myInformation' });
	  };
	  return {
		route,
		bouncingPath,
		toIndex,
		toOrderList,
		toMyInformation,
	  };
	},
  });
  </script>

  <style scoped>
  .footer {
	width: 100%;
	height: 72px;
	border-top: 1px solid rgba(var(--skin-brand-soft-rgb, 180, 211, 230), 0.7);
	background: rgba(255, 255, 255, .94);
	box-shadow: 0 -8px 22px rgba(var(--skin-muted-rgb, 58, 112, 150), 0.08);
	backdrop-filter: blur(14px);

	position: fixed;
	left: 0;
	bottom: 0;

	display: flex;
	justify-content: space-around;
	align-items: center;
	z-index: 1000;
  }

  .footer > li {
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	color: var(--skin-muted, #91a0aa);
	user-select: none;
	cursor: pointer;
	flex: 1; /* 使每个 li 平均分配宽度 */
  }

  .footer > li p {
	margin-top: 4px;
	font-size: 12px;
	font-weight: 600;
  }

	.footer > li i {
	font-size: 25px;
	line-height: 1;
	transform-origin: center;
	transition: transform 200ms cubic-bezier(.22, 1, .36, 1);
	}

	.footer > li.active {
		color: var(--skin-brand, #168fe4);
	}

	.footer > li.active i {
		transform: translateY(-3px) scale(1.08);
	}

	/* 点击底部导航：图标向上弹跳一次 */
	.footer > li.is-bouncing i {
		animation: nav-bounce 200ms cubic-bezier(.22, 1, .36, 1);
	}

	@keyframes nav-bounce {
		0% { transform: translateY(0) scale(1); }
		45% { transform: translateY(-5px) scale(1.06); }
		100% { transform: translateY(0) scale(1); }
	}

	@media (prefers-reduced-motion: reduce) {
		.footer > li i,
		.footer > li.active i {
			transition: none;
		}

		.footer > li.is-bouncing i {
			animation: none;
		}
	}

	.footer > li.active p {
		font-weight: 800;
	}
</style>
