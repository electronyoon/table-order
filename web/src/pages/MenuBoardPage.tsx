import { useParams } from 'react-router-dom'

function MenuBoardPage() {
  const { qrToken } = useParams<{ qrToken: string }>()

  return (
    <div>
      <p>메뉴판 준비 중</p>
      <p>qrToken: {qrToken}</p>
    </div>
  )
}

export default MenuBoardPage
