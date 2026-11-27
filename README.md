# HAMA AI

An independent Android AI assistant project.

## Current app features
- Online AI chat powered by Cloudflare Workers AI
- 40-language selector plus automatic language detection
- Replies in the selected language or the language of the user's message
- Android app source code and GitHub Actions APK build workflow
- No OpenAI API key is required

## AI model
The Cloudflare Worker uses `@cf/zai-org/glm-4.7-flash`, a multilingual text-generation model documented for 100+ languages.

## Deploy the Cloudflare Worker

1. Open Cloudflare Dashboard → Workers & Pages.
2. Open the existing Worker named `cool-brook-c584`.
3. Open its code editor and replace the script with `cloudflare-worker.js` from this repository.
4. Open Settings → Bindings (or Variables and Secrets, depending on the dashboard view).
5. Add a **Workers AI** binding with the variable name `AI`.
6. Save and Deploy.
7. Test: open `https://cool-brook-c584.khosthroe892.workers.dev`; the server should respond that it is running and uses `POST /chat`.

No `OPENAI_API_KEY` secret is needed. The Worker uses the Cloudflare Workers AI binding.

## Free usage note
Cloudflare Workers AI currently includes a shared daily free allocation of 10,000 Neurons. Requests stop when the daily free allocation is exhausted; model availability and limits can change. See the official [Workers AI pricing](https://developers.cloudflare.com/workers-ai/platform/pricing/).

## Build the Android APK
The GitHub Actions workflow is `.github/workflows/android.yml`. Open the repository's Actions tab, choose **Build HAMA AI APK**, and run the workflow. Download the `HAMA-AI-APK` artifact after the workflow succeeds.
