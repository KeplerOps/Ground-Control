import json
import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
CATALOG_PATH = REPO_ROOT / "packs" / "catalog.json"


class PackCatalogTest(unittest.TestCase):
    def test_catalog_entries_are_unique_and_well_formed(self):
        catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))

        self.assertEqual(catalog["project"], "ground-control")
        seen_ids = set()

        for entry in catalog["packs"]:
            pack_id = entry["packId"]
            self.assertNotIn(pack_id, seen_ids)
            seen_ids.add(pack_id)

            self.assertEqual(entry["format"], "OSCAL_JSON")
            self.assertEqual(entry["defaultControlFunction"], "PREVENTIVE")
            self.assertTrue(entry["sourceUrl"].startswith("https://raw.githubusercontent.com/usnistgov/oscal-content/"))
            self.assertNotIn("file", entry)
            self.assertRegex(entry["sourceSha256"], r"^[0-9a-f]{64}$")


if __name__ == "__main__":
    unittest.main()
