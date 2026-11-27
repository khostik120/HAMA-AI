# HAMA AI — Ultimate Edition

## Product goals

HAMA AI is an Android AI assistant with a clean, accessible chat interface, local conversation storage, optional speech input/output, and a modular path toward multimodal and offline capabilities.

## Current implementation in this repository

- Chat screen with selectable response language.
- Conversation history stored locally using Android SharedPreferences.
- Copy and share actions on messages; assistant messages can be read with Android TextToSpeech.
- Speech-to-text input through Android SpeechRecognizer (device/provider support required).
- Persistent local Projects list (create and delete).
- Local Library index using Android Storage Access Framework (add, open, remove document references).
- Attach supported text, JSON, XML, and source-code files to a chat prompt (up to 60,000 characters); file content is sent to the configured online Worker.
- Export and import a JSON backup of conversation history, project names, Library URI references, profile name, selected language, and auto-speech setting; imports merge chats, projects, and Library entries without replacing matching IDs/names/URIs.
- Remember the selected response language across app restarts.
- Note: Library backup stores Android document URI references, not copies of the actual files; those references may not work on another device or after provider access changes.
- Cancel an in-progress online AI request from the send button.
- Profile name stored on device.
- Settings for response language, automatic spoken replies, and clearing chat history.
- Online chat backend configured through the Cloudflare Worker endpoint in MainActivity.

## Capability roadmap

| Capability | Status | Notes |
|---|---|---|
| Text chat | Implemented | Uses configured remote Worker endpoint |
| Conversation history | Implemented | Stored on device |
| Speech input | Implemented | Android speech service and microphone permission required |
| Text-to-speech | Implemented | Uses installed Android TTS voices |
| Projects | Basic local implementation | Project names are stored locally |
| File Library | Basic local implementation | Stores document URI references; chat attachment reads supported text/code formats up to 60,000 characters |
| Image understanding | Not implemented | Requires image-capable model and secure backend upload |
| Image generation | Not implemented | Requires a generation model/service |
| Video generation/analysis | Not implemented | Requires dedicated model/service and substantial compute |
| Web search | Not implemented | Requires a search provider/backend |
| Coding tools | Not implemented | Requires model/tool execution design and sandboxing |
| On-device/offline LLM | Not implemented | Requires selecting, packaging/downloading, and benchmarking a compatible model |
| Streaming responses | Not implemented | Current client reads a complete JSON response |
| App data backup | Implemented | JSON export/import for chats, projects, Library references, profile name, language, and auto-speech; actual Library file bytes are not copied |
| Cancel AI request | Implemented | Disconnects the active HTTP request; cancellation behavior still needs device testing |
| Encrypted local storage | Not implemented | Requires key management and migration plan |

## Engineering requirements

1. Do not embed provider API secrets in the Android application. Keep credentials on a trusted backend.
2. Treat uploaded files as untrusted input. Validate MIME type and size; do not execute uploaded content.
3. Ask permission before microphone, camera, or file access; use Android's system document picker where possible.
4. Make network calls cancellable, bounded by timeouts, and resilient to offline conditions.
5. Keep large language models optional and measure memory, storage, battery, and latency on target devices.
6. Add automated unit/instrumentation tests and verify a release build before publishing an APK.

## ZIP and model size

A source ZIP size is not a quality metric. Do not add filler code or duplicated data to inflate archives. If an offline model is included, document its origin, license, checksum, actual size, supported languages, and device requirements. Keep model binaries separate from the source archive when licensing or distribution size requires it.

## Release acceptance checklist

- [ ] Gradle build succeeds in CI.
- [ ] Install and launch on a physical Android device.
- [ ] Verify chat success, server errors, timeout, and no-network states.
- [ ] Verify history persistence after app restart.
- [ ] Verify project create/delete persistence.
- [ ] Verify document add/open/remove flows.
- [ ] Verify supported text/code attachment, empty file, oversized file, and unsupported MIME type.
- [ ] Verify JSON backup export/import and duplicate merge behavior for chats, projects, Library references, and settings.
- [ ] Verify restored Android document URI references on the same device and confirm expected behavior on another device.
- [ ] Verify cancelling before connection, during upload, and while waiting for the server response.
- [ ] Verify speech permission denied/granted flows.
- [ ] Verify accessibility, RTL, small screens, and keyboard behavior.
- [ ] Verify no secrets are committed.
