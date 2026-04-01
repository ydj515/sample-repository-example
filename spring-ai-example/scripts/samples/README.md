# Sample Upload Files

`scripts/samples/`는 `multimodal`, `audio` 예제 호출에 사용할 업로드 파일을 두는 디렉터리입니다.

기본 파일명:

- `sample-image.png`
- `sample-audio.mp3`

예시:

```bash
cp /absolute/path/my-image.png scripts/samples/sample-image.png
cp /absolute/path/my-audio.mp3 scripts/samples/sample-audio.mp3
./scripts/test-curl.sh
```

환경변수로 다른 파일을 지정할 수도 있습니다.

```bash
IMAGE_FILE=/absolute/path/other-image.png \
AUDIO_FILE=/absolute/path/other-audio.mp3 \
./scripts/test-curl.sh
```
