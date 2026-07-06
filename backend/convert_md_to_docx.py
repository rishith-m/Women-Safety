import os
import pypandoc

md_file = r'C:\Users\RISHI\.gemini\antigravity\brain\5e6b5bda-e3a2-47cf-8edb-b36b807bcba2\automated_xray_screening_proposal.md'
docx_file = r'C:\Users\RISHI\.gemini\antigravity\brain\5e6b5bda-e3a2-47cf-8edb-b36b807bcba2\automated_xray_screening_proposal.docx'

print("Downloading pandoc...")
pypandoc.download_pandoc()

print("Converting markdown to docx...")
output = pypandoc.convert_file(md_file, 'docx', outputfile=docx_file)

print(f"Conversion complete. Saved to {docx_file}")
