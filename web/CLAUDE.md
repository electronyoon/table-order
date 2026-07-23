# web

Vite + React + TypeScript, PWA(`vite-plugin-pwa`). 소비자(QR 손님) 전용 PWA.

## 빌드/테스트

- 설치: `npm install`
- 개발 서버: `npm run dev`
- 빌드: `npm run build` (`tsc -b && vite build`)

## 코드 컨벤션

- 라우트는 `src/App.tsx`에 등록하고, 페이지 컴포넌트는 `src/pages/`에 둔다.
- 소비자 API(`/t/{qrToken}/**`)만 호출한다 — `/admin/**`는 android 전용.

## 금지사항

- 백엔드 도메인 로직(품절 판정, 상태 전이 등)을 프론트에서 재구현하지 않는다 — 항상 서버 API 응답을 신뢰한다.
- API 변경 시 [contracts/openapi.yaml](../contracts/openapi.yaml)을 먼저 수정하고 여기서 따라간다.
