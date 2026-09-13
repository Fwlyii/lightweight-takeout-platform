<template>
	<ul class="footer">
	  <li :class="{ active: route.path === '/index' }" @click="toIndex">
		<i class="fa fa-home"></i>
		<p>首页</p>
	  </li>
	  <li :class="{ active: route.path === '/orderList' }" @click="toOrderList">
		<i class="fa fa-file-text-o"></i>
		<p>订单</p>
	  </li>
	  <li :class="{ active: route.path === '/myInformation' }" @click="toMyInformation">
		<i class="fa fa-user-o"></i>
		<p>我的</p>
	  </li>
	</ul>
  </template>
  
  <script>
  import { defineComponent } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  export default defineComponent({
	name: 'Footer',
	setup() {
	  const router = useRouter();
	  const route = useRoute();
	  const toIndex = () => {
		router.push({ path: '/index' });
	  };
  
	  const toOrderList = () => {
		router.push({ path: '/orderList' });
	  };
	  const toMyInformation = () => {
		router.push({ path: '/myInformation' });
	  };
	  return {
		route,
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
	border-top: 1px solid rgba(180, 211, 230, .7);
	background: rgba(255, 255, 255, .94);
	box-shadow: 0 -8px 22px rgba(58, 112, 150, .08);
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
	color: #91a0aa;
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
		color: #168fe4;
	}

	.footer > li.active i {
		transform: translateY(-3px) scale(1.08);
	}

	.footer > li.active p {
		font-weight: 800;
	}
</style>
