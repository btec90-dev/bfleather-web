import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged } from "firebase/auth";
import { getFirestore, doc, collection, onSnapshot, setDoc, addDoc, deleteDoc, updateDoc } from "firebase/firestore";

if (typeof document !== 'undefined' && !document.getElementById('tw-cdn')) {
  const s = document.createElement('script');
  s.id = 'tw-cdn';
  s.src = 'https://tailwindcss.com';
  document.head.appendChild(s);
}

const firebaseConfig = {
  apiKey: "AIzaSyA3ZSsDUbLi7G71JLRrfiOMwNGkUnpaNDE",
  authDomain: "://firebaseapp.com",
  projectId: "gen-lang-client-0700116335",
  storageBucket: "gen-lang-client-0700116335.firebasestorage.app",
  messagingSenderId: "1033796007852",
  appId: "1:1033796007852:web:ks7hgtqlk0gv5a4kankps30l1l2usmjr"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app, "ai-studio-android-monparco-7fd85cbb-8596-4fb8-9521-c1253e90a815");
export const googleProvider = new GoogleAuthProvider();

export { signInWithPopup, signOut, onAuthStateChanged, doc, collection, onSnapshot, setDoc, addDoc, deleteDoc, updateDoc };
