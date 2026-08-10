import './UserProfile.css'
import ProfileHeader from './components/ProfileHeader'
import ContributionGraph from './components/ContributionGraph'
import SkillMatrixChart from './components/SkillMatrixChart'
import EditProfileModal from './components/EditProfileModal'

const UserProfile = () => {
  return (
    <main className="">
      <div className="__container">
        <h1>User Profile</h1>
        <ProfileHeader />
        <ContributionGraph />
        <SkillMatrixChart />
        <EditProfileModal />
      </div>
    </main>
  )
}

export default UserProfile

