# BharatCall — ফোন থেকে Render সেটআপ (বাংলা)

এই ধাপগুলো Android ফোনের Chrome browser থেকেই করা যাবে। কোনো terminal command লাগবে না। ₹0 রাখবে; Free ছাড়া অন্য plan নির্বাচন করবে না।

## আগে যা জানা জরুরি

এই deployment শুধু BharatCall-এর mock backend চালু করে এবং Android app-এর health-check পরীক্ষা করতে সাহায্য করে। এটি SIM call করে না, কলের audio নেয় না, Gemini-কে live call-এ যুক্ত করে না। বাস্তব AI calling এখনও তৈরি হয়নি।

## Render-এ কী করবে

1. Chrome-এ https://dashboard.render.com/ খোলো এবং GitHub দিয়ে sign in করো।
2. **New +** চাপো। **Blueprint** option থাকলে সেটি বেছে নাও। না থাকলে **New Web Service** বেছে নিয়ে repo connect করার ধাপ অনুসরণ করো।
3. GitHub repositories থেকে **cryding/BharatCall** connect করো।
4. Branch হিসেবে **ai-telephony-bridge** বেছে নাও। **main** নয়—কারণ নতুন backend এই branch-এ আছে।
5. Blueprint ব্যবহারে render.yaml পড়ে service তৈরি করার preview দেখাবে। Service name bharatcall-telephony-api; plan অবশ্যই **Free** থাকবে। Paid plan, paid instance, paid add-on বা billing upgrade বেছে নিও না।
6. Environment variable চাইলে BHARATCALL_API_TOKEN-এর জন্য 32 অক্ষরের বেশি একটি random secret দাও। এটি private রাখবে—GitHub file, chat, screenshot বা Android app-এ দেবে না। যদি Render-এ field না আসে, service-এর **Environment** tab-এ গিয়ে Add Environment Variable ব্যবহার করো।
7. **Create Blueprint / Deploy** চাপো। Build/deploy শেষ হয়ে service Live/Healthy হওয়া পর্যন্ত অপেক্ষা করো। প্রথমবার কিছু সময় লাগতে পারে; Free service idle থাকলে পরে জেগে উঠতে দেরি হতে পারে।
8. Render service-এর onrender.com URL কপি করো। URL-এর শেষে /health যোগ করে Chrome-এ খোলো। উদাহরণ: https://your-service.onrender.com/health
9. JSON-এ ok: true এবং telephonyConnected: false দেখলে mock backend health check সফল। telephonyConnected false হওয়াই প্রত্যাশিত—এখনও কোনো phone provider বা media connection নেই।
10. BharatCall app খোলো → **Settings** → **AI telephony backend URL** → কপি করা base URL (শেষে /health নয়) paste করে **Save URL** চাপো।
11. তারপর **Test telephony backend** চাপো। Success dialog এলে app থেকে server পর্যন্ত health check কাজ করছে।

## সমস্যা হলে

- **Repository দেখা যাচ্ছে না:** Render-এর GitHub connection-এ BharatCall repo access অনুমোদন করো।
- **Branch খুঁজে পাচ্ছ না:** branch name ঠিক ai-telephony-bridge কি না দেখো।
- **Build failed:** Render-এর Events/Logs-এ error দেখো; plan upgrade করো না।
- **Health URL-এ 404:** URL-এর শেষে /health আছে কি না দেখো; শুধু base URL app-এর Settings-এ save করবে।
- **Service sleeping / প্রথম request slow:** Free service idle হলে startup delay হতে পারে; কিছুক্ষণ অপেক্ষা করে আবার চেষ্টা করো।
- **Token:** Android app-এ server-এর shared token বসানোর প্রয়োজন নেই। Public APK-তে এই secret কখনো embed করা যাবে না।

## Render deploy হওয়ার পরের development

এরপর নিরাপদে implement করার ধাপ হলো app-এর backend health/status UI ও mock session flow যাচাই করা। Protected API-তে Android থেকে session চালু করার আগে per-user login এবং short-lived access token তৈরি করতে হবে; server-এর shared secret APK-তে রাখা যাবে না। বাস্তব SIM/PSTN AI calling-এর জন্য আলাদা provider/media gateway এবং অনুমোদিত end-to-end test দরকার, যার খরচ হতে পারে। Paid provider/billing চালু করা হবে না.