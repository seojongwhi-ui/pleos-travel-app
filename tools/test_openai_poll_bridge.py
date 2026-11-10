import io
import json
import unittest
from unittest.mock import patch

from tools.openai_poll_bridge import make_poll


class PollBridgeTest(unittest.TestCase):
    def test_missing_key_is_clear(self):
        with patch.dict("os.environ", {"OPENAI_API_KEY": ""}):
            with self.assertRaisesRegex(ValueError, "OPENAI_API_KEY"):
                make_poll("어디서 만날까")

    def test_structured_response_becomes_poll(self):
        response = {"status": "completed", "output": [{"type": "message", "content": [
            {"type": "output_text", "text": '{"title":"어디서 쉴까요?","options":["휴게소","카페"]}'}]}]}
        with patch.dict("os.environ", {"OPENAI_API_KEY": "test-only"}), \
                patch("tools.openai_poll_bridge.urlopen", return_value=io.BytesIO(json.dumps(response).encode())) as send:
            poll = make_poll("휴게소랑 카페 중에서 쉬는 곳 투표하자")
        self.assertEqual(poll["options"], ["휴게소", "카페"])
        body = json.loads(send.call_args.args[0].data)
        self.assertEqual(body["text"]["format"]["type"], "json_schema")
        self.assertFalse(body["store"])


if __name__ == "__main__":
    unittest.main()
