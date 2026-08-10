import './DocumentAnalyzer.css'
import FileUploader from './components/FileUploader'
import DocumentSummary from './components/DocumentSummary'
import GeneratedNotes from './components/GeneratedNotes'

const DocumentAnalyzer = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Document Analyzer</h1>
        <FileUploader />
        <DocumentSummary />
        <GeneratedNotes />
      </div>
    </main>
  )
}

export default DocumentAnalyzer

