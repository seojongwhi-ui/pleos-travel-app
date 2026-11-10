"""Local-only OpenAI poll bridge for the LinkCar Android emulator.

Run with OPENAI_API_KEY in this process's environment. Android connects to
http://10.0.2.2:8765; the key never enters the APK or repository.
"""

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def make_poll(transcript: str) -> dict:
    key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not key:
        raise ValueError("호스트에 OPENAI_API_KEY가 설정되지 않았습니다.")
    if not transcript.strip() or len(transcript) > 500:
        raise ValueError("투표 제안은 1~500자로 말해 주세요.")
    payload = {
        "model": os.environ.get("OPENAI_POLL_MODEL", "gpt-4o-mini"),
        "store": False,
        "instructions": (
            "너는 차량 동행 앱의 투표 안건 정리 도우미다. 사용자의 한국어 음성 인식 결과에서 "
            "한 개의 짧고 중립적인 투표 질문과 선택지 2~4개를 한국어로 만든다. "
            "사용자가 말하지 않은 사실, 장소, 가격은 지어내지 않는다. "
            "운전자에게 조작을 요구하거나 안전하지 않은 행동을 권하지 않는다."
        ),
        "input": transcript.strip(),
        "text": {"format": {
            "type": "json_schema", "name": "linkcar_poll", "strict": True,
            "schema": {
                "type": "object",
                "properties": {
                    "title": {"type": "string"},
                    "options": {"type": "array", "items": {"type": "string"}},
                },
                "required": ["title", "options"],
                "additionalProperties": False,
            },
        }},
    }
    request = Request(
        "https://api.openai.com/v1/responses",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=25) as response:
            result = json.load(response)
    except HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        try:
            detail = json.loads(body).get("error", {}).get("message", body)
        except ValueError:
            detail = body
        raise RuntimeError(f"OpenAI API {exc.code}: {detail[:300]}") from exc
    except URLError as exc:
        raise RuntimeError(f"OpenAI API 연결 실패: {exc.reason}") from exc
    if result.get("status") != "completed":
        raise RuntimeError(f"OpenAI 응답 미완료: {result.get('status', 'unknown')}")
    texts = [part.get("text", "") for item in result.get("output", [])
             if item.get("type") == "message"
             for part in item.get("content", []) if part.get("type") == "output_text"]
    if not texts:
        raise RuntimeError("OpenAI 응답에 투표 안건이 없습니다.")
    try:
        poll = json.loads("".join(texts))
    except ValueError as exc:
        raise RuntimeError("OpenAI 투표 응답을 읽지 못했습니다.") from exc
    title = poll.get("title", "").strip()
    options = list(dict.fromkeys(option.strip() for option in poll.get("options", [])))
    if not title or len(options) not in range(2, 5) or not all(options):
        raise RuntimeError("OpenAI가 유효한 투표 안건을 만들지 못했습니다.")
    return {"title": title, "options": options}


class Handler(BaseHTTPRequestHandler):
    def send_json(self, status: int, data: dict) -> None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        if self.path == "/health":
            self.send_json(200, {"ready": bool(os.environ.get("OPENAI_API_KEY"))})
        else:
            self.send_json(404, {"error": "경로를 찾을 수 없습니다."})

    def do_POST(self) -> None:
        if self.path != "/poll":
            self.send_json(404, {"error": "경로를 찾을 수 없습니다."})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > 4096:
            self.send_json(413, {"error": "요청 크기가 올바르지 않습니다."})
            return
        try:
            body = json.loads(self.rfile.read(length))
            self.send_json(200, make_poll(str(body.get("transcript", ""))))
        except (ValueError, RuntimeError) as exc:
            self.send_json(400 if isinstance(exc, ValueError) else 502, {"error": str(exc)})
        except Exception:
            self.send_json(500, {"error": "투표 생성 중 예기치 않은 오류가 발생했습니다."})


if __name__ == "__main__":
    print("LinkCar OpenAI poll bridge: http://127.0.0.1:8765", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
