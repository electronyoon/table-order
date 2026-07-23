import { Routes, Route } from 'react-router-dom'
import MenuBoardPage from './pages/MenuBoardPage'

function App() {
  return (
    <Routes>
      <Route path="/t/:qrToken" element={<MenuBoardPage />} />
    </Routes>
  )
}

export default App
