import DustTool from "./tools/DustTool.js";
import { DATA_PORTAL_API_KEY } from "./config.js";

async function main() {
  try {
    if (!DATA_PORTAL_API_KEY) {
      throw new Error(
        "DATA_PORTAL_API_KEY가 설정되어 있지 않습니다. .env에 키를 추가해주세요."
      );
    }

    const args = process.argv[2];
    const input = args ? JSON.parse(args) : {};

    const tool = new DustTool(DATA_PORTAL_API_KEY);
    const result = await tool.execute(input as any);

    console.log(JSON.stringify(result, null, 2));
  } catch (err: any) {
    console.error("DustTool error:", err?.message || err);
    process.exit(1);
  }
}

main();
