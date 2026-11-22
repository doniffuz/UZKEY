import os, subprocess, sys, tempfile, unittest
sys.path.insert(0, os.path.dirname(__file__))
import build_dictionary as b

class T(unittest.TestCase):
    def test_apostrophes(self):
        self.assertEqual(b.normalize("Oʻzbek O’zbek o`zbek o‘zbek"), "o'zbek o'zbek o'zbek o'zbek")
    def test_cyrillic(self):
        self.assertEqual(b.cyr_to_latin("ўзбек қишлоқ ғалла ҳаво"), "o'zbek qishloq g'alla havo")
        self.assertEqual(b.cyr_to_latin("Ишга шаҳар хат"), "Ishga shahar xat")
    def test_tokenize_drops_digits(self):
        self.assertEqual(list(b.tokenize_sentences("men 5 ta kitob o'qidim. u keldi")), [["men","ta","kitob","o'qidim"],["u","keldi"]])
    def test_min_count_and_order(self):
        with tempfile.TemporaryDirectory() as d:
            c=os.path.join(d,"c.txt"); open(c,"w",encoding="utf-8").write("men ishga boraman. men ishga bordim. men uyga boraman.\n")
            subprocess.run([sys.executable,os.path.join(os.path.dirname(__file__),"build_dictionary.py"),c,"-o",d,"--min-count","2"],check=True,capture_output=True)
            w=open(os.path.join(d,"words_uz.txt"),encoding="utf-8").read().splitlines()
            self.assertEqual(w[0],"men\t3"); self.assertNotIn("uyga\t1",w); self.assertIn("ishga\t2",w)
if __name__=="__main__": unittest.main()
