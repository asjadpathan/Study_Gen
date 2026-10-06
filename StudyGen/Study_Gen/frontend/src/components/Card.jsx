import React from 'react';
import styles from './Card.module.css';

const Card = ({ children, className = '', hoverable = true, ...props }) => {
  return (
    <div
      className={`${styles.card} ${hoverable ? styles.hoverable : ''} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};

export default Card;
