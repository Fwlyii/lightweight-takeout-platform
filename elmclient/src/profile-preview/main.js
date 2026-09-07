import {createApp,h} from 'vue';
import {createRouter,createWebHistory,RouterView} from 'vue-router';
import {installAuthGuard} from '../router/authGuard';
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import Session from '../auth-preview/Session.vue';
import MyInformation from '../views/MyInformation.vue';
import UserAddress from '../views/UserAddress.vue';
import AddUserAddress from '../views/AddUserAddress.vue';
import EditUserAddress from '../views/EditUserAddress.vue';
import Favorites from '../views/Favorites.vue';
import Catalog from './Catalog.vue';
import '../auth-preview/preview.css';

// Feature acceptance entry: real production components/API; no simulated payment or delivery.
const router=createRouter({history:createWebHistory(),routes:[
  {path:'/login',name:'Login',component:Login,meta:{public:true}},
  {path:'/register',name:'Register',component:Register,meta:{public:true}},
  {path:'/index',redirect:'/profile-preview/stores'},
  {path:'/',redirect:'/myInformation'},
  {path:'/myInformation',name:'MyInformation',component:MyInformation,meta:{role:'user'}},
  {path:'/userAddress',name:'UserAddress',component:UserAddress,meta:{role:'user'}},
  {path:'/addUserAddress',name:'AddUserAddress',component:AddUserAddress,meta:{role:'user'}},
  {path:'/editUserAddress',name:'EditUserAddress',component:EditUserAddress,meta:{role:'user'}},
  {path:'/favorites',name:'Favorites',component:Favorites,meta:{role:'user'}},
  {path:'/profile-preview/stores',component:Catalog,meta:{role:'user'}},
  {path:'/auth/session',component:Session},
  {path:'/:pathMatch(.*)*',redirect:'/auth/session'}
]});
installAuthGuard(router);
createApp({render:()=>h(RouterView)}).use(router).mount('#app');
