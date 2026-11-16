import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = "/Users/eugene/Documents/estimate/SERP-POC.xlsx";
const outDir = "/private/tmp/serp-poc-inspect";
await fs.mkdir(outDir, { recursive: true });

const input = await FileBlob.load(inputPath);
const workbook = await SpreadsheetFile.importXlsx(input);

const sheets = await workbook.inspect({
  kind: "sheet",
  include: "id,name",
  maxChars: 5000,
});
console.log("SHEETS\n" + sheets.ndjson);

for (const sheet of workbook.worksheets.items) {
  const used = sheet.getUsedRange();
  console.log(`\nUSED ${sheet.name}: ${used?.address ?? "none"}`);
  if (!used) continue;
  const region = await workbook.inspect({
    kind: "region",
    sheetId: sheet.name,
    range: used.address,
    maxChars: 18000,
    tableMaxRows: 80,
    tableMaxCols: 12,
    tableMaxCellChars: 500,
  });
  console.log(`REGION ${sheet.name}\n${region.ndjson}`);

  const preview = await workbook.render({
    sheetName: sheet.name,
    autoCrop: "all",
    scale: 1,
    format: "png",
  });
  await fs.writeFile(`${outDir}/${sheet.name.replaceAll("/", "_")}.png`, new Uint8Array(await preview.arrayBuffer()));
}
