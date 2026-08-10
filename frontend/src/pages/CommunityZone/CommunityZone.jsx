import './CommunityZone.css'
import ChannelSidebar from './components/ChannelSidebar'
import ChatWindow from './components/ChatWindow'
import PostThread from './components/PostThread'

const CommunityZone = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>Community Zone</h1>
        <ChannelSidebar />
        <ChatWindow />
        <PostThread />
      </div>
    </main>
  )
}

export default CommunityZone

